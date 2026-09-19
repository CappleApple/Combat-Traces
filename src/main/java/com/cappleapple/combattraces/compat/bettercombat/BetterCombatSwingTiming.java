package com.cappleapple.combattraces.compat.bettercombat;

import com.cappleapple.combattraces.api.SwingWindow;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation.KeyFrame;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation.StateCollection;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation.StateCollection.State;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Infers a visual strike from authored motion; PlayerAnimator has no damaging-phase marker. */
public final class BetterCombatSwingTiming {
  private static final double MIN_MOVEMENT = 0.025;
  private static final double CONTINUATION_SPEED = 0.12;

  private BetterCombatSwingTiming() {}

  public static SwingWindow resolve(KeyframeAnimation animation, float hitProgress, boolean spin) {
    if (animation == null || animation.endTick <= 0 || animation.isInfinite) {
      return SwingWindow.around(hitProgress);
    }
    double contact =
        Float.isFinite(hitProgress)
            ? Math.clamp(hitProgress, 0, 1) * animation.endTick
            : animation.endTick * 0.5;
    List<Channel> channels = new ArrayList<>();
    channels.addAll(channels(animation.getPart("rightArm"), 1, false));
    channels.addAll(channels(animation.getPart("rightItem"), 1, false));
    // Mirrored offhand playback uses the canonical right-hand tracks. Left-only custom clips still
    // need a usable motion source, but the free arm must not extend a normal right-hand recovery.
    if (movement(channels, 0, animation.endTick) < MIN_MOVEMENT) {
      channels.addAll(channels(animation.getPart("leftArm"), 1, false));
      channels.addAll(channels(animation.getPart("leftItem"), 1, false));
    }
    channels.addAll(channels(animation.getPart("torso"), .7, true));
    channels.addAll(channels(animation.getPart("body"), .7, true));

    SwingWindow result = strikingMotion(channels, animation.endTick, contact);
    SwingWindow rotation = rotationWindow(channels, animation.endTick, contact, spin);
    if (rotation != null
        && (spin
            || rotation.contains((float) (contact / animation.endTick))
            || result == null
            || !result.contains((float) (contact / animation.endTick)))) {
      return rotation;
    }
    return result == null ? SwingWindow.around(hitProgress) : result;
  }

  private static SwingWindow strikingMotion(List<Channel> channels, int length, double contact) {
    TreeSet<Integer> boundaries = new TreeSet<>();
    for (Channel channel : channels) {
      for (KeyFrame frame : channel.state.getKeyFrames()) {
        if (frame.tick >= 0 && frame.tick <= length) boundaries.add(frame.tick);
      }
    }
    List<Integer> ticks = new ArrayList<>(boundaries);
    List<Segment> segments = new ArrayList<>();
    int best = -1;
    double bestScore = 0;
    for (int i = 1; i < ticks.size(); i++) {
      int start = ticks.get(i - 1), end = ticks.get(i);
      double[] vector = new double[channels.size()];
      for (int j = 0; j < channels.size(); j++) vector[j] = channels.get(j).motion(start, end);
      Segment segment = new Segment(start, end, vector);
      segments.add(segment);
      if (segment.amount() < MIN_MOVEMENT) continue;
      // Contact chooses the stroke, not an artificial endpoint within that stroke.
      double score = segment.speed() * proximity(start, end, contact, length);
      if (score > bestScore) {
        best = segments.size() - 1;
        bestScore = score;
      }
    }
    if (best < 0) return null;
    int first = best, last = best;
    double threshold = segments.get(best).speed() * CONTINUATION_SPEED;
    while (first > 0 && continues(segments.get(first - 1), segments.get(first), threshold)) first--;
    while (last + 1 < segments.size()
        && continues(segments.get(last + 1), segments.get(last), threshold)) last++;
    return window(segments.get(first).start, segments.get(last).end, length);
  }

  private static boolean continues(Segment candidate, Segment previous, double threshold) {
    if (candidate.amount() < MIN_MOVEMENT || candidate.speed() < threshold) return false;
    double continuing = 0, reversing = 0;
    for (int i = 0; i < candidate.vector.length; i++) {
      double current = candidate.vector[i], prior = previous.vector[i];
      if (Math.abs(prior) < .001) continuing += Math.abs(current) * .5;
      else if (current * prior >= 0) continuing += Math.abs(current);
      else reversing += Math.abs(current);
    }
    // Changing joints can carry one continuous slash: arm pitch may ease off as torso yaw and
    // wrist rotation carry the blade through. A substantial reversal starts a separate movement.
    return continuing >= reversing;
  }

  private static SwingWindow rotationWindow(
      List<Channel> channels, int length, double contact, boolean spin) {
    List<Revolution> revolutions = new ArrayList<>();
    for (Channel channel : channels) {
      if (!channel.rotation) continue;
      List<KeyFrame> frames = channel.state.getKeyFrames();
      List<Rotation> rotations = new ArrayList<>();
      double peakSpeed = 0;
      for (int i = 1; i < frames.size(); i++) {
        KeyFrame before = frames.get(i - 1), after = frames.get(i);
        if (before.tick < 0 || after.tick > length || after.tick <= before.tick) continue;
        double delta = channel.phaseDelta(before.value, after.value);
        Rotation rotation = new Rotation(before.tick, after.tick, delta);
        rotations.add(rotation);
        peakSpeed = Math.max(peakSpeed, rotation.speed());
      }
      double threshold = peakSpeed * .15;
      for (int first = 0; first < rotations.size(); ) {
        Rotation start = rotations.get(first);
        if (start.speed() < threshold || Math.abs(start.delta) < .005) {
          first++;
          continue;
        }
        int last = first;
        double total = Math.abs(start.delta);
        while (last + 1 < rotations.size()) {
          Rotation previous = rotations.get(last), next = rotations.get(last + 1);
          if (previous.end != next.start
              || next.speed() < threshold
              || previous.delta * next.delta <= 0) break;
          total += Math.abs(next.delta);
          last++;
        }
        // Full item rotations matter even when the hitbox is not a 360 degree sweep: staff
        // finishers and animated chakram throws continue after the arm reaches its held pose.
        double minimumTurn = channel.body && !spin ? Math.PI * 1.5 : Math.PI;
        if (total >= minimumTurn) {
          revolutions.add(new Revolution(start.start, rotations.get(last).end, total));
        }
        first = last + 1;
      }
    }
    Revolution best = null;
    double bestScore = 0;
    for (Revolution revolution : revolutions) {
      double score =
          revolution.amount * proximity(revolution.start, revolution.end, contact, length);
      if (score > bestScore) {
        best = revolution;
        bestScore = score;
      }
    }
    if (best == null) return null;
    int start = best.start, end = best.end;
    boolean extended;
    do {
      extended = false;
      for (Revolution revolution : revolutions) {
        if (revolution.end < start || revolution.start > end) continue;
        int nextStart = Math.min(start, revolution.start), nextEnd = Math.max(end, revolution.end);
        extended |= nextStart != start || nextEnd != end;
        start = nextStart;
        end = nextEnd;
      }
    } while (extended);
    return window(start, end, length);
  }

  private static double proximity(int start, int end, double contact, int length) {
    double distance = Math.max(start - contact, Math.max(contact - end, 0));
    return start < contact && contact <= end
        ? 4
        : 1 + .2 / (1 + distance / Math.max(1, length * .1));
  }

  private static double movement(List<Channel> channels, int start, int end) {
    double amount = 0;
    for (Channel channel : channels) {
      List<KeyFrame> frames = channel.state.getKeyFrames();
      for (int i = 1; i < frames.size(); i++) {
        KeyFrame before = frames.get(i - 1), after = frames.get(i);
        int duration = after.tick - before.tick;
        int overlap = Math.min(after.tick, end) - Math.max(before.tick, start);
        if (duration > 0 && overlap > 0)
          amount += Math.abs(channel.delta(before.value, after.value)) * overlap / duration;
      }
    }
    return amount;
  }

  private static List<Channel> channels(StateCollection part, double weight, boolean body) {
    if (part == null) return List.of();
    List<Channel> result = new ArrayList<>();
    for (State state : new State[] {part.pitch, part.yaw, part.roll, part.bend}) {
      if (state != null && state.isEnabled()) result.add(new Channel(state, true, weight, body));
    }
    for (State state : List.of(part.x, part.y, part.z)) {
      if (state.isEnabled()) result.add(new Channel(state, false, weight, body));
    }
    return result;
  }

  private static SwingWindow window(int start, int end, int length) {
    return new SwingWindow((float) start / length, (float) end / length);
  }

  private record Segment(int start, int end, double[] vector) {
    double amount() {
      double amount = 0;
      for (double value : vector) amount += Math.abs(value);
      return amount;
    }

    double speed() {
      return amount() / (end - start);
    }
  }

  private record Rotation(int start, int end, double delta) {
    double speed() {
      return Math.abs(delta) / (end - start);
    }
  }

  private record Revolution(int start, int end, double amount) {}

  private record Channel(State state, boolean rotation, double weight, boolean body) {
    double delta(double before, double after) {
      // PlayerAnimator interpolates raw angles before wrapping the result. Preserve those turns
      // when measuring motion; normalizing endpoint differences could erase an entire revolution.
      return (rotation ? after - before : (after - before) / 16) * weight;
    }

    double phaseDelta(double before, double after) {
      double value = after - before;
      // Principal-angle crossings belong to the same authored body spin. This groups its phase;
      // it does not change the raw transforms sampled or rendered by Combat Traces.
      if (body
          && Math.abs(before) <= Math.PI + .001
          && Math.abs(after) <= Math.PI + .001
          && Math.abs(value) > Math.PI) {
        value -= Math.copySign(Math.PI * 2, value);
      }
      return value;
    }

    double motion(int start, int end) {
      double amount = 0;
      List<KeyFrame> frames = state.getKeyFrames();
      for (int i = 1; i < frames.size(); i++) {
        KeyFrame before = frames.get(i - 1), after = frames.get(i);
        int duration = after.tick - before.tick;
        int overlap = Math.min(after.tick, end) - Math.max(before.tick, start);
        if (duration > 0 && overlap > 0)
          amount += delta(before.value, after.value) * overlap / duration;
      }
      return amount;
    }
  }
}
