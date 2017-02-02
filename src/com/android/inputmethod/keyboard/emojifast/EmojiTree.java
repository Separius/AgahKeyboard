package com.android.inputmethod.keyboard.emojifast;

/**
 * Created by sepehr on 2/1/17.
 */

import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Based in part on code from emoji-java
 */
public class EmojiTree {

    private final EmojiTreeNode root = new EmojiTreeNode();

    public void add(String emojiEncoding, EmojiDrawInfo emoji) {
        EmojiTreeNode tree = root;

        for (char c: emojiEncoding.toCharArray()) {
            if (!tree.hasChild(c)) {
                tree.addChild(c);
            }

            tree = tree.getChild(c);
        }

        tree.setEmoji(emoji);
    }

    public Matches isEmoji(CharSequence sequence, int startPosition, int endPosition) {
        if (sequence == null) {
            return Matches.POSSIBLY;
        }

        EmojiTreeNode tree = root;

        for (int i=startPosition; i<endPosition; i++) {
            char character = sequence.charAt(i);

            if (!tree.hasChild(character)) {
                return Matches.IMPOSSIBLE;
            }

            tree = tree.getChild(character);
        }

        return tree.isEndOfEmoji() ? Matches.EXACTLY : Matches.POSSIBLY;
    }

    public @Nullable EmojiDrawInfo getEmoji(CharSequence unicode, int startPosition, int endPostiion) {
        EmojiTreeNode tree = root;

        for (int i=startPosition; i<endPostiion; i++) {
            char character = unicode.charAt(i);

            if (!tree.hasChild(character)) {
                return null;
            }

            tree = tree.getChild(character);
        }

        return tree.getEmoji();
    }


    private static class EmojiTreeNode {

        private Map<Character, EmojiTreeNode> children = new HashMap<>();
        private EmojiDrawInfo emoji;

        public void setEmoji(EmojiDrawInfo emoji) {
            this.emoji = emoji;
        }

        public @Nullable EmojiDrawInfo getEmoji() {
            return emoji;
        }

        boolean hasChild(char child) {
            return children.containsKey(child);
        }

        void addChild(char child) {
            children.put(child, new EmojiTreeNode());
        }

        EmojiTreeNode getChild(char child) {
            return children.get(child);
        }

        boolean isEndOfEmoji() {
            return emoji != null;
        }
    }

    public enum Matches {
        EXACTLY, POSSIBLY, IMPOSSIBLE;

        public boolean exactMatch() {
            return this == EXACTLY;
        }

        public boolean impossibleMatch() {
            return this == IMPOSSIBLE;
        }
    }

}
