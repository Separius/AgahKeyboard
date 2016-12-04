/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.latin.makedict;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.separ.neural.inputmethod.annotations.UsedForTesting;
import io.separ.neural.inputmethod.indic.Constants;

/**
 * Dictionary File Format Specification.
 */
public final class FormatSpec {

    /*
     * File header layout is as follows:
     *
     * v |
     * e | MAGIC_NUMBER + version of the file format, 2 bytes.
     * r |
     * sion
     *
     * o |
     * p | not used                                3 bits
     * t | each unigram and bigram entry has a time stamp?
     * i |                                         1 bit, 1 = yes, 0 = no : CONTAINS_TIMESTAMP_FLAG
     * o |
     * nflags
     *
     * h |
     * e | size of the file header, 4bytes
     * a |   including the size of the magic number, the option flags and the header size
     * d |
     * ersize
     *
     *   | attributes list
     *
     * attributes list is:
     * <key>   = | string of characters at the char format described below, with the terminator used
     *           | to signal the end of the string.
     * <value> = | string of characters at the char format described below, with the terminator used
     *           | to signal the end of the string.
     * if the size of already read < headersize, goto key.
     *
     */

    /*
     * Node array (FusionDictionary.PtNodeArray) layout is as follows:
     *
     * n |
     * o | the number of PtNodes, 1 or 2 bytes.
     * d | 1 byte = bbbbbbbb match
     * e |   case 1xxxxxxx => xxxxxxx << 8 + next byte
     * c |   otherwise => bbbbbbbb
     * o |
     * unt
     *
     * n |
     * o | sequence of PtNodes,
     * d | the layout of each PtNode is described below.
     * e |
     * s
     *
     * f |
     * o | forward link address, 3byte
     * r | 1 byte = bbbbbbbb match
     * w |   case 1xxxxxxx => -((xxxxxxx << 16) + (next byte << 8) + next byte)
     * a |   otherwise => (xxxxxxx << 16) + (next byte << 8) + next byte
     * r |
     * dlinkaddress
     */

    /* Node (FusionDictionary.PtNode) layout is as follows:
     *   | is moved ?             2 bits, 11 = no          : FLAG_IS_NOT_MOVED
     *   |                          This must be the same as FLAG_CHILDREN_ADDRESS_TYPE_THREEBYTES
     *   |                                01 = yes         : FLAG_IS_MOVED
     * f |                      the new address is stored in the same place as the parent address
     * l | is deleted?                    10 = yes         : FLAG_IS_DELETED
     * a | has several chars ?         1 bit, 1 = yes, 0 = no   : FLAG_HAS_MULTIPLE_CHARS
     * g | has a terminal ?            1 bit, 1 = yes, 0 = no   : FLAG_IS_TERMINAL
     * s | has shortcut targets ?      1 bit, 1 = yes, 0 = no   : FLAG_HAS_SHORTCUT_TARGETS
     *   | has bigrams ?               1 bit, 1 = yes, 0 = no   : FLAG_HAS_BIGRAMS
     *   | is not a word ?             1 bit, 1 = yes, 0 = no   : FLAG_IS_NOT_A_WORD
     *   | is blacklisted ?            1 bit, 1 = yes, 0 = no   : FLAG_IS_BLACKLISTED
     *
     * p |
     * a | parent address, 3byte
     * r | 1 byte = bbbbbbbb match
     * e |   case 1xxxxxxx => -((0xxxxxxx << 16) + (next byte << 8) + next byte)
     * n |   otherwise => (bbbbbbbb << 16) + (next byte << 8) + next byte
     * t | This address is relative to the head of the PtNode.
     * a | If the node doesn't have a parent, this field is set to 0.
     * d |
     * dress
     *
     * c | IF FLAG_HAS_MULTIPLE_CHARS
     * h |   char, char, char, char    n * (1 or 3 bytes) : use PtNodeInfo for i/o helpers
     * a |   end                       1 byte, = 0
     * r | ELSE
     * s |   char                      1 or 3 bytes
     *   | END
     *
     * f |
     * r | IF FLAG_IS_TERMINAL
     * e |   frequency                 1 byte
     * q |
     *
     * c |
     * h | children address, 3 bytes
     * i | 1 byte = bbbbbbbb match
     * l |   case 1xxxxxxx => -((0xxxxxxx << 16) + (next byte << 8) + next byte)
     * d |   otherwise => (bbbbbbbb<<16) + (next byte << 8) + next byte
     * r | if this node doesn't have children, this field is set to 0.
     * e |   (see BinaryDictEncoderUtils#writeVariableSignedAddress)
     * n | This address is relative to the position of this field.
     * a |
     * ddress
     *
     *   | IF FLAG_IS_TERMINAL && FLAG_HAS_SHORTCUT_TARGETS
     *   | shortcut string list
     *   | IF FLAG_IS_TERMINAL && FLAG_HAS_BIGRAMS
     *   | bigrams address list
     *
     * Char format is:
     * 1 byte = bbbbbbbb match
     * case 000xxxxx: xxxxx << 16 + next byte << 8 + next byte
     * else: if 00011111 (= 0x1F) : this is the terminator. This is a relevant choice because
     *       unicode code points range from 0 to 0x10FFFF, so any 3-byte value starting with
     *       00011111 would be outside unicode.
     * else: iso-latin-1 code
     * This allows for the whole unicode range to be encoded, including chars outside of
     * the BMP. Also everything in the iso-latin-1 charset is only 1 byte, except control
     * characters which should never happen anyway (and still work, but take 3 bytes).
     *
     * bigram address list is:
     * <flags> = | hasNext = 1 bit, 1 = yes, 0 = no     : FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT
     *           | addressSign = 1 bit,                 : FLAG_BIGRAM_ATTR_OFFSET_NEGATIVE
     *           |                      1 = must take -address, 0 = must take +address
     *           |                         xx : mask with MASK_BIGRAM_ATTR_ADDRESS_TYPE
     *           | addressFormat = 2 bits, 00 = unused  : FLAG_BIGRAM_ATTR_ADDRESS_TYPE_ONEBYTE
     *           |                         01 = 1 byte  : FLAG_BIGRAM_ATTR_ADDRESS_TYPE_ONEBYTE
     *           |                         10 = 2 bytes : FLAG_BIGRAM_ATTR_ADDRESS_TYPE_TWOBYTES
     *           |                         11 = 3 bytes : FLAG_BIGRAM_ATTR_ADDRESS_TYPE_THREEBYTES
     *           | 4 bits : frequency         : mask with FLAG_BIGRAM_SHORTCUT_ATTR_FREQUENCY
     * <address> | IF (01 == FLAG_BIGRAM_ATTR_ADDRESS_TYPE_ONEBYTE == addressFormat)
     *           |   read 1 byte, add top 4 bits
     *           | ELSIF (10 == FLAG_BIGRAM_ATTR_ADDRESS_TYPE_TWOBYTES == addressFormat)
     *           |   read 2 bytes, add top 4 bits
     *           | ELSE // 11 == FLAG_BIGRAM_ATTR_ADDRESS_TYPE_THREEBYTES == addressFormat
     *           |   read 3 bytes, add top 4 bits
     *           | END
     *           | if (FLAG_BIGRAM_ATTR_OFFSET_NEGATIVE) then address = -address
     * if (FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT) goto bigram_and_shortcut_address_list_is
     *
     * shortcut string list is:
     * <byte size> = PTNODE_SHORTCUT_LIST_SIZE_SIZE bytes, big-endian: size of the list, in bytes.
     * <flags>     = | hasNext = 1 bit, 1 = yes, 0 = no : FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT
     *               | reserved = 3 bits, must be 0
     *               | 4 bits : frequency : mask with FLAG_BIGRAM_SHORTCUT_ATTR_FREQUENCY
     * <shortcut>  = | string of characters at the char format described above, with the terminator
     *               | used to signal the end of the string.
     * if (FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT goto flags
     */

    public static final int MAGIC_NUMBER = 0x9BC13AFE;
    static final int NOT_A_VERSION_NUMBER = -1;
    static final int FIRST_VERSION_WITH_DYNAMIC_UPDATE = 3;
    static final int FIRST_VERSION_WITH_TERMINAL_ID = 4;

    // These MUST have the same values as the relevant constants in format_utils.h.
    // From version 4 on, we use version * 100 + revision as a version number. That allows
    // us to change the format during development while having testing devices remove
    // older files with each upgrade, while still having a readable versioning scheme.
    // When we bump up the dictionary format version, we should update
    // ExpandableDictionary.needsToMigrateDictionary() and
    // ExpandableDictionary.matchesExpectedBinaryDictFormatVersionForThisType().
    public static final int VERSION2 = 2;
    // Dictionary version used for testing.
    public static final int VERSION4_ONLY_FOR_TESTING = 399;
    public static final int VERSION401 = 401;
    public static final int VERSION4 = 402;
    public static final int VERSION4_DEV = 403;
    static final int MINIMUM_SUPPORTED_VERSION = VERSION2;
    static final int MAXIMUM_SUPPORTED_VERSION = VERSION4_DEV;

    // TODO: Make this value adaptative to content data, store it in the header, and
    // use it in the reading code.
    static final int MAX_WORD_LENGTH = Constants.DICTIONARY_MAX_WORD_LENGTH;

    static final int PARENT_ADDRESS_SIZE = 3;
    static final int FORWARD_LINK_ADDRESS_SIZE = 3;

    // These flags are used only in the static dictionary.
    static final int MASK_CHILDREN_ADDRESS_TYPE = 0xC0;
    static final int FLAG_CHILDREN_ADDRESS_TYPE_NOADDRESS = 0x00;
    static final int FLAG_CHILDREN_ADDRESS_TYPE_ONEBYTE = 0x40;
    static final int FLAG_CHILDREN_ADDRESS_TYPE_TWOBYTES = 0x80;
    static final int FLAG_CHILDREN_ADDRESS_TYPE_THREEBYTES = 0xC0;

    static final int FLAG_HAS_MULTIPLE_CHARS = 0x20;

    static final int FLAG_IS_TERMINAL = 0x10;
    static final int FLAG_HAS_SHORTCUT_TARGETS = 0x08;
    static final int FLAG_HAS_BIGRAMS = 0x04;
    static final int FLAG_IS_NOT_A_WORD = 0x02;
    static final int FLAG_IS_BLACKLISTED = 0x01;

    // These flags are used only in the dynamic dictionary.
    static final int MASK_MOVE_AND_DELETE_FLAG = 0xC0;
    static final int FIXED_BIT_OF_DYNAMIC_UPDATE_MOVE = 0x40;
    static final int FLAG_IS_MOVED = FIXED_BIT_OF_DYNAMIC_UPDATE_MOVE;
    static final int FLAG_IS_NOT_MOVED = 0x80 | FIXED_BIT_OF_DYNAMIC_UPDATE_MOVE;
    static final int FLAG_IS_DELETED = 0x80;

    static final int FLAG_BIGRAM_SHORTCUT_ATTR_HAS_NEXT = 0x80;
    static final int FLAG_BIGRAM_ATTR_OFFSET_NEGATIVE = 0x40;
    static final int MASK_BIGRAM_ATTR_ADDRESS_TYPE = 0x30;
    static final int FLAG_BIGRAM_ATTR_ADDRESS_TYPE_ONEBYTE = 0x10;
    static final int FLAG_BIGRAM_ATTR_ADDRESS_TYPE_TWOBYTES = 0x20;
    static final int FLAG_BIGRAM_ATTR_ADDRESS_TYPE_THREEBYTES = 0x30;
    static final int FLAG_BIGRAM_SHORTCUT_ATTR_FREQUENCY = 0x0F;

    static final int PTNODE_CHARACTERS_TERMINATOR = 0x1F;

    static final int PTNODE_TERMINATOR_SIZE = 1;
    static final int PTNODE_FLAGS_SIZE = 1;
    static final int PTNODE_FREQUENCY_SIZE = 1;
    static final int PTNODE_TERMINAL_ID_SIZE = 4;
    static final int PTNODE_MAX_ADDRESS_SIZE = 3;
    static final int PTNODE_ATTRIBUTE_FLAGS_SIZE = 1;
    static final int PTNODE_ATTRIBUTE_MAX_ADDRESS_SIZE = 3;
    static final int PTNODE_SHORTCUT_LIST_SIZE_SIZE = 2;

    // These values are used only by version 4 or later. They MUST match the definitions in
    // ver4_dict_constants.cpp.
    static final String TRIE_FILE_EXTENSION = ".trie";
    public static final String HEADER_FILE_EXTENSION = ".header";
    static final String FREQ_FILE_EXTENSION = ".freq";
    // tat = Terminal Address Table
    static final String TERMINAL_ADDRESS_TABLE_FILE_EXTENSION = ".tat";
    static final String BIGRAM_FILE_EXTENSION = ".bigram";
    static final String SHORTCUT_FILE_EXTENSION = ".shortcut";
    static final String LOOKUP_TABLE_FILE_SUFFIX = "_lookup";
    static final String CONTENT_TABLE_FILE_SUFFIX = "_index";
    static final int FLAGS_IN_FREQ_FILE_SIZE = 1;
    static final int FREQUENCY_AND_FLAGS_SIZE = 2;
    static final int TERMINAL_ADDRESS_TABLE_ADDRESS_SIZE = 3;
    static final int UNIGRAM_TIMESTAMP_SIZE = 4;
    static final int UNIGRAM_COUNTER_SIZE = 1;
    static final int UNIGRAM_LEVEL_SIZE = 1;

    // With the English main dictionary as of October 2013, the size of bigram address table is
    // is 345KB with the block size being 16.
    // This is 54% of that of full address table.
    static final int BIGRAM_ADDRESS_TABLE_BLOCK_SIZE = 16;
    static final int BIGRAM_CONTENT_COUNT = 1;
    static final int BIGRAM_FREQ_CONTENT_INDEX = 0;
    static final String BIGRAM_FREQ_CONTENT_ID = "_freq";
    static final int BIGRAM_TIMESTAMP_SIZE = 4;
    static final int BIGRAM_COUNTER_SIZE = 1;
    static final int BIGRAM_LEVEL_SIZE = 1;

    static final int SHORTCUT_CONTENT_COUNT = 1;
    static final int SHORTCUT_CONTENT_INDEX = 0;
    // With the English main dictionary as of October 2013, the size of shortcut address table is
    // 26KB with the block size being 64.
    // This is only 4.4% of that of full address table.
    static final int SHORTCUT_ADDRESS_TABLE_BLOCK_SIZE = 64;
    static final String SHORTCUT_CONTENT_ID = "_shortcut";

    static final int NO_CHILDREN_ADDRESS = Integer.MIN_VALUE;
    static final int NO_PARENT_ADDRESS = 0;
    static final int NO_FORWARD_LINK_ADDRESS = 0;
    static final int INVALID_CHARACTER = -1;

    static final int MAX_PTNODES_FOR_ONE_BYTE_PTNODE_COUNT = 0x7F; // 127
    // Large PtNode array size field size is 2 bytes.
    static final int LARGE_PTNODE_ARRAY_SIZE_FIELD_SIZE_FLAG = 0x8000;
    static final int MAX_PTNODES_IN_A_PT_NODE_ARRAY = 0x7FFF; // 32767
    static final int MAX_BIGRAMS_IN_A_PTNODE = 10000;
    static final int MAX_SHORTCUT_LIST_SIZE_IN_A_PTNODE = 0xFFFF;

    static final int MAX_TERMINAL_FREQUENCY = 255;
    static final int MAX_BIGRAM_FREQUENCY = 15;

    public static final int SHORTCUT_WHITELIST_FREQUENCY = 15;

    // This option needs to be the same numeric value as the one in binary_format.h.
    static final int NOT_VALID_WORD = -99;
    static final int SIGNED_CHILDREN_ADDRESS_SIZE = 3;

    static final int UINT8_MAX = 0xFF;
    static final int UINT16_MAX = 0xFFFF;
    static final int UINT24_MAX = 0xFFFFFF;
    static final int SINT24_MAX = 0x7FFFFF;
    static final int MSB8 = 0x80;
    static final int MSB24 = 0x800000;

    /**
     * Options about file format.
     */
    public static final class FormatOptions {
        public final int mVersion;
        public final boolean mHasTimestamp;

        @UsedForTesting
        public FormatOptions(final int version) {
            this(version, false /* hasTimestamp */);
        }

        public FormatOptions(final int version, final boolean hasTimestamp) {
            mVersion = version;
            mHasTimestamp = hasTimestamp;
        }
    }

    /**
     * Options global to the dictionary.
     */
    public static final class DictionaryOptions {
        public final HashMap<String, String> mAttributes;
        public DictionaryOptions(final HashMap<String, String> attributes) {
            mAttributes = attributes;
        }
        @Override
        public String toString() { // Convenience method
            return toString(0, false);
        }
        public String toString(final int indentCount, final boolean plumbing) {
            final StringBuilder indent = new StringBuilder();
            if (plumbing) {
                indent.append("H:");
            } else {
                for (int i = 0; i < indentCount; ++i) {
                    indent.append(' ');
                }
            }
            final StringBuilder s = new StringBuilder();
            for (final Map.Entry<String, String> stringStringEntry : mAttributes.entrySet()) {
                s.append(indent);
                s.append(stringStringEntry.getKey());
                s.append(" = ");
                if ("date".equals(stringStringEntry.getKey()) && !plumbing) {
                    // Date needs a number of milliseconds, but the dictionary contains seconds
                    s.append(new Date(
                            1000 * Long.parseLong(stringStringEntry.getValue())).toString());
                } else {
                    s.append(stringStringEntry.getValue());
                }
                s.append('\n');
            }
            return s.toString();
        }
    }

    private FormatSpec() {
        // This utility class is not publicly instantiable.
    }
}
