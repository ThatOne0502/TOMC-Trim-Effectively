package io.github.thatone0502.trimeffectively.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import io.github.thatone0502.trimeffectively.data.EffectEntry;
import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

/**
 * {@code trimeffectively:annotate_sound} — the ward pattern lets this client *see* the sounds around the wearer.
 *
 * <p>Everything here is client local: the data is synced, the sounds are the ones this client is already playing, and
 * nothing is sent over the network. A sound that the warden could not hear (blocked by the {@code blocked_by} block
 * tag, {@code minecraft:wool} in the data, which is exactly what blocks vanilla vibrations) is never annotated.
 *
 * <p>Each annotation is a vibration particle travelling from the sound towards this client's player plus a billboarded
 * subtitle label. The label is drawn in a HUD pass rather than as a world entity: the data is per client, the label must
 * always face *this* camera, be readable through walls and be invisible to everyone else, and a {@code TextDisplay}
 * entity can do none of those (and would need to be synced).
 */
public final class SoundAnnotations {
	public static final String TYPE = "annotate_sound";
	public static final String TRIGGER_SOUND = "sound_emitted";

	private static final int MS_PER_TICK = 50;
	private static final int MAX_LABELS = 48;

	private static final List<Marker> MARKERS = new ArrayList<>();

	public static void register() {
		HudRenderCallback.EVENT.register((graphics, delta) -> render(graphics));
	}

	/**
	 * Called for every sound this client starts playing, from {@code SoundManager#play} — the one funnel that sees both
	 * sounds sent by the server and sounds predicted locally.
	 */
	public static void onSoundPlayed(SoundInstance instance) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null || instance == null) {
			return;
		}
		// Player-bound sounds have no world position of their own.
		if (instance.isRelative() || quiet(instance.getSource())) {
			return;
		}
		TrimClientIndex.ensure(minecraft);
		List<TrimEffects.Active> actives = actives(player);
		if (actives.isEmpty()) {
			return;
		}
		Vec3 at = new Vec3(instance.getX(), instance.getY(), instance.getZ());
		Vec3 eye = player.getEyePosition();
		Identifier soundId = soundId(instance);
		String fallback = soundId == null ? null : soundId.toString();
		for (TrimEffects.Active active : actives) {
			EffectEntry entry = active.entry();
			if (!entry.client()) {
				continue;
			}
			double radius = entry.radius() == null ? 8.0D : entry.radius().eval(active.pieces(), Map.of());
			if (at.distanceTo(eye) > radius) {
				continue;
			}
			if (entry.excludeOwn() && own(instance, player, at)) {
				continue;
			}
			if (blocked(minecraft.level, at, eye, entry.blockedBy())) {
				continue;
			}
			EffectEntry.Label label = entry.label();
			int visibleMs = Math.max(0, label == null ? 40 : label.visibleTicks()) * MS_PER_TICK;
			int fadeMs = Math.max(1, label == null ? 60 : label.fadeTicks()) * MS_PER_TICK;
			String text = label == null || label.subtitle() ? subtitle(minecraft, soundId, fallback) : fallback;
			if (text == null || text.isBlank()) {
				text = "?";
			}
			push(new Marker(at, text, System.currentTimeMillis(), visibleMs, fadeMs));
			vibration(minecraft, player, at, entry);
			return;
		}
	}

	/**
	 * Sounds a warden does not perceive (music, ambience, interface) would only spam the screen. Voice is kept out for
	 * the same reason: it never travels as a world sound a warden hears.
	 */
	private static boolean quiet(SoundSource source) {
		if (source == null) {
			return true;
		}
		return source == SoundSource.MASTER
				|| source == SoundSource.MUSIC
				|| source == SoundSource.AMBIENT
				|| source == SoundSource.UI
				|| source == SoundSource.VOICE;
	}

	private static List<TrimEffects.Active> actives(LocalPlayer player) {
		List<TrimEffects.Active> sound = TrimEffects.activeOfType(player, TRIGGER_SOUND, TYPE);
		if (!sound.isEmpty()) {
			return sound;
		}
		return TrimEffects.activeOfType(player, EffectEntry.DEFAULT_TRIGGER, TYPE);
	}

	/**
	 * Sounds the local player emitted are not annotated. There is no sound class to test any more in 1.21.11, so this
	 * recognises the player-bound instance by name and falls back to "the sound starts on me".
	 *
	 * <p>The fallback compares against the player's feet, not the eyes: a sound played on the player is positioned at
	 * {@code player.position()} - the middle of the block the player stands in - which is a full eye height away from
	 * the camera. Measuring from the eye is why footsteps and weapon swings used to slip through.
	 */
	private static boolean own(SoundInstance instance, LocalPlayer player, Vec3 at) {
		String name = instance.getClass().getSimpleName();
		if (name.contains("LocalPlayer") || name.endsWith("LocalSoundInstance")) {
			return true;
		}
		return at.distanceToSqr(player.getX(), player.getY(), player.getZ()) < 1.0D;
	}

	/**
	 * Sound occlusion, the warden way: only blocks carrying the {@code blocked_by} tag stop a sound, and every sampled
	 * block along the line from the sound to the listener is tested, because a wall of stone is not a wall of wool.
	 */
	private static boolean blocked(Level level, Vec3 from, Vec3 to, Identifier tagId) {
		if (tagId == null || level == null) {
			return false;
		}
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		if (length < 1.0E-4D) {
			return false;
		}
		TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagId);
		int steps = (int) Math.ceil(length * 2.0D);
		for (int step = 1; step < steps; step++) {
			double ratio = (double) step / (double) steps;
			BlockState state = level.getBlockState(BlockPos.containing(
					from.x + delta.x * ratio,
					from.y + delta.y * ratio,
					from.z + delta.z * ratio));
			if (state.is(tag)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The subtitle vanilla itself would show for this sound.
	 *
	 * <p>{@code SoundEvents} carries no subtitle at runtime, but the loaded {@code sounds.json} entry does:
	 * {@code SoundManager#getSoundEvent} returns the {@code WeighedSoundEvents} built from it, and anything with a
	 * {@code "subtitle"} field exposes it as a component. That is why a footstep reads as vanilla's own
	 * "脚步声"/"Footsteps" instead of a language key - the key for a step sound is not derivable from the sound id,
	 * because many ids share one subtitle.
	 *
	 * <p>Only when the pack declares no subtitle does the id-based key get tried, and the raw id is the last resort.
	 */
	private static String subtitle(Minecraft minecraft, Identifier soundId, String fallback) {
		if (soundId == null) {
			return fallback;
		}
		try {
			WeighedSoundEvents event = minecraft.getSoundManager().getSoundEvent(soundId);
			if (event != null && event != SoundManager.INTENTIONALLY_EMPTY_SOUND_EVENT) {
				Component declared = event.getSubtitle();
				if (declared != null) {
					String text = declared.getString();
					if (text != null && !text.isEmpty()) {
						return text;
					}
				}
			}
		} catch (RuntimeException ignored) {
			// A pack that never declared the event simply falls through to the id.
		}
		String key = subtitleKey(soundId);
		String translated = Component.translatable(key).getString();
		if (translated != null && !translated.isEmpty() && !translated.equals(key)) {
			return translated;
		}
		return fallback;
	}

	/**
	 * Vanilla drops the namespace for its own sounds: the subtitle of {@code minecraft:block.stone.step} is
	 * {@code subtitles.block.stone.step}, not {@code subtitles.minecraft.block.stone.step}. Building the key with the
	 * namespace always attached silently missed every vanilla sound and fell back to the raw id.
	 */
	private static String subtitleKey(Identifier soundId) {
		if ("minecraft".equals(soundId.getNamespace())) {
			return "subtitles." + soundId.getPath();
		}
		return "subtitles." + soundId.getNamespace() + "." + soundId.getPath();
	}

	/** The particle the warden uses: it leaves the sound and travels to this client's player. */
	private static void vibration(Minecraft minecraft, LocalPlayer player, Vec3 at, EffectEntry entry) {
		if (minecraft.level == null) {
			return;
		}
		Identifier particle = entry.particle();
		if (particle != null && !particle.getPath().equals("vibration")) {
			return;
		}
		double distance = at.distanceTo(player.position());
		int arrival = (int) Math.max(2.0D, Math.round(distance * 2.0D));
		PositionSource destination = new EntityPositionSource(player, player.getBbHeight() * 0.5F);
		minecraft.level.addParticle(new VibrationParticleOption(destination, arrival), at.x, at.y, at.z, 0.0D, 0.0D, 0.0D);
	}

	/**
	 * A repeated sound (footsteps, a mob's idle noise) must not pile up labels: a label with the same text at the same
	 * spot is restarted in place, so a walking wearer keeps one label per sound instead of one per step.
	 */
	private static void push(Marker marker) {
		for (int i = 0; i < MARKERS.size(); i++) {
			Marker existing = MARKERS.get(i);
			if (existing.text().equals(marker.text()) && existing.pos().distanceTo(marker.pos()) < 1.0D) {
				MARKERS.set(i, marker);
				return;
			}
		}
		while (MARKERS.size() >= MAX_LABELS) {
			MARKERS.remove(0);
		}
		MARKERS.add(marker);
	}

	/**
	 * Billboarded, see-through labels. The world position is projected into the camera pose by hand so the label lives
	 * purely in screen space: it faces this camera by construction, it is not depth tested (so walls do not hide it,
	 * {@code see_through}), and its alpha is driven by the label timings from the data.
	 */
	private static void render(GuiGraphics graphics) {
		if (MARKERS.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			MARKERS.clear();
			return;
		}
		TrimClientIndex.ensure(minecraft);
		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();
		if (width <= 0 || height <= 0) {
			return;
		}
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.position();
		Quaternionf inverse = new Quaternionf(camera.rotation()).conjugate();
		Integer fov = minecraft.options.fov().get();
		double degrees = fov == null || fov.intValue() <= 1 ? 70.0D : fov.intValue();
		double focal = 1.0D / Math.tan(Math.toRadians(degrees) * 0.5D);
		double aspect = (double) width / (double) height;
		long now = System.currentTimeMillis();
		Iterator<Marker> iterator = MARKERS.iterator();
		while (iterator.hasNext()) {
			Marker marker = iterator.next();
			long age = now - marker.startMillis();
			long total = (long) marker.visibleMs() + (long) marker.fadeMs();
			if (age >= total) {
				iterator.remove();
				continue;
			}
			int alpha = 255;
			if (age > marker.visibleMs()) {
				double faded = (double) (age - marker.visibleMs()) / (double) Math.max(1, marker.fadeMs());
				alpha = (int) Math.round(255.0D * Math.max(0.0D, 1.0D - faded));
			}
			if (alpha <= 2) {
				continue;
			}
			Vector3f view = new Vector3f(
					(float) (marker.pos().x - cameraPos.x),
					(float) (marker.pos().y - cameraPos.y),
					(float) (marker.pos().z - cameraPos.z));
			inverse.transform(view);
			if (view.z >= -0.05F) {
				continue;
			}
			double ndcX = (view.x * focal / aspect) / (double) (-view.z);
			double ndcY = (view.y * focal) / (double) (-view.z);
			if (ndcX < -1.5D || ndcX > 1.5D || ndcY < -1.5D || ndcY > 1.5D) {
				continue;
			}
			float screenX = (float) ((ndcX * 0.5D + 0.5D) * (double) width);
			float screenY = (float) ((-ndcY * 0.5D + 0.5D) * (double) height);
			double distance = marker.pos().distanceTo(cameraPos);
			// Larger than a name tag on purpose: the point is to be read at a glance while fighting.
			float scale = (float) Math.max(0.8D, Math.min(2.4D, 5.0D / Math.max(0.75D, distance)));
			// Sculk cyan rather than white: the dimension this effect is built around is dark, and white text
			// on dark stone reads as a smudge at a distance.
			int colour = (alpha & 0xFF) << 24 | 0x00FFFF;
			int textWidth = minecraft.font.width(marker.text());
			// Sounds emitted from the same spot (a whole pack of mobs stepping together) would otherwise stack
			// exactly on top of each other and read as one garbled line. The offset is derived from the marker
			// itself, so one label keeps a stable position for its whole life instead of jittering per frame.
			int seed = marker.text().hashCode() * 31 + (int) (marker.startMillis() & 0xFFFFL);
			float jitterX = (float) ((seed % 5) - 2) * 14.0F;
			float jitterY = (float) (((seed / 5) % 5) - 2) * 10.0F;
			Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			pose.translate(screenX + jitterX, screenY + jitterY);
			pose.scale(scale, scale);
			// A dark plate behind the glyphs so the label survives being drawn over stone, sculk growth or a lit
			// lava wall; it fades with the text so the whole label is one unit. The cyan rim keeps the edge of the
			// plate readable when the background behind it is already bright.
			int halfWidth = textWidth / 2 + 3;
			int halfHeight = minecraft.font.lineHeight / 2 + 2;
			graphics.fill(-halfWidth, -halfHeight, halfWidth, halfHeight, (((int) (alpha * 0.55D)) << 24));
			int rim = (((int) (alpha * 0.75D)) << 24) | 0x00FFFF;
			graphics.fill(-halfWidth, -halfHeight, halfWidth, -halfHeight + 1, rim);
			graphics.fill(-halfWidth, halfHeight - 1, halfWidth, halfHeight, rim);
			graphics.fill(-halfWidth, -halfHeight, -halfWidth + 1, halfHeight, rim);
			graphics.fill(halfWidth - 1, -halfHeight, halfWidth, halfHeight, rim);
			graphics.drawString(minecraft.font, marker.text(), -textWidth / 2, -minecraft.font.lineHeight / 2, colour, true);
			pose.popMatrix();
		}
	}

	/**
	 * The sound's registry id.
	 *
	 * <p>{@code getSound()} is only populated once the engine has resolved the instance against the loaded
	 * sound set, which is not yet true at the point the play call is made, so the id is read from the
	 * instance itself instead: every vanilla instance is an {@code AbstractSoundInstance}, which keeps the
	 * identifier it was constructed with.
	 */
	private static Identifier soundId(SoundInstance instance) {
		if (instance instanceof AbstractSoundInstance abstractSound) {
			return abstractSound.getIdentifier();
		}
		Sound sound = instance.getSound();
		return sound == null ? null : sound.getLocation();
	}

	/** One live label: where the sound was, what to show, when it started and how long each phase lasts. */
	private record Marker(Vec3 pos, String text, long startMillis, int visibleMs, int fadeMs) {
	}

	private SoundAnnotations() {
	}
}
