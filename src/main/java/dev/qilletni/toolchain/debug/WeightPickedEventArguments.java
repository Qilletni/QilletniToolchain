package dev.qilletni.toolchain.debug;

import dev.qilletni.impl.debug.WeightPick;

import java.util.List;

/**
 * The body of a {@code qilletni/weightPicked} event. See {@link WeightPick} for what each field means.
 */
public class WeightPickedEventArguments {

    public int weightsId;
    public String label;
    public String kind;
    public List<Level> path;
    public String song;
    public String trackId;
    public List<Rejected> rejected;
    public int rejectedOmitted;
    public Breakdown breakdown;

    public static class Level {
        public int index;
        public String name;
        public double roll;
        public double from;
        public double to;
        public double chance;
    }

    public static class Rejected {
        public List<Integer> indices;
        public List<String> path;
        public String song;
        public String reason;
    }

    public static class Breakdown {
        public List<Node> roots;
        public double miss;
        public int omitted;
        public double omittedChance;
    }

    public static class Node {
        public String name;
        public int index;
        public String kind;
        public double chance;
        public double combined;
        public String source;
        public int listSize;
        public int songCount;
        public List<Node> children;
    }

    public static WeightPickedEventArguments of(WeightPick pick) {
        var args = new WeightPickedEventArguments();
        args.weightsId = pick.weightsId();
        args.label = pick.label();
        args.kind = pick.kind();
        args.path = pick.path().stream().map(WeightPickedEventArguments::level).toList();
        args.song = pick.song();
        args.trackId = pick.trackId();
        args.rejected = pick.rejected().stream().map(WeightPickedEventArguments::rejected).toList();
        args.rejectedOmitted = pick.rejectedOmitted();
        args.breakdown = pick.breakdown() == null ? null : breakdown(pick.breakdown());
        return args;
    }

    private static Level level(WeightPick.Level from) {
        var level = new Level();
        level.index = from.index();
        level.name = from.name();
        level.roll = from.roll();
        level.from = from.from();
        level.to = from.to();
        level.chance = from.chance();
        return level;
    }

    private static Rejected rejected(WeightPick.Rejected from) {
        var rejected = new Rejected();
        rejected.indices = from.indices();
        rejected.path = from.path();
        rejected.song = from.song();
        rejected.reason = from.reason();
        return rejected;
    }

    private static Breakdown breakdown(WeightPick.Breakdown from) {
        var breakdown = new Breakdown();
        breakdown.roots = from.roots().stream().map(WeightPickedEventArguments::node).toList();
        breakdown.miss = from.miss();
        breakdown.omitted = from.omitted();
        breakdown.omittedChance = from.omittedChance();
        return breakdown;
    }

    private static Node node(WeightPick.Node from) {
        var node = new Node();
        node.name = from.name();
        node.index = from.index();
        node.kind = from.kind();
        node.chance = from.chance();
        node.combined = from.combined();
        node.source = from.source();
        node.listSize = from.listSize();
        node.songCount = from.songCount();
        node.children = from.children().stream().map(WeightPickedEventArguments::node).toList();
        return node;
    }
}
