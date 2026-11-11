package com.sahalos.sboard.core

import com.sahalos.sboard.model.KeyModel
import com.sahalos.sboard.model.KeyType

object KeyLayoutProvider {

    fun getQwertyRows(): List<List<KeyModel>> {
        val row1 = listOf(
            KeyModel('q'.code, "q", "1", 1.0f),
            KeyModel('w'.code, "w", "2", 1.0f),
            KeyModel('e'.code, "e", "3", 1.0f),
            KeyModel('r'.code, "r", "4", 1.0f),
            KeyModel('t'.code, "t", "5", 1.0f),
            KeyModel('y'.code, "y", "6", 1.0f),
            KeyModel('u'.code, "u", "7", 1.0f),
            KeyModel('i'.code, "i", "8", 1.0f),
            KeyModel('o'.code, "o", "9", 1.0f),
            KeyModel('p'.code, "p", "0", 1.0f)
        )

        val row2 = listOf(
            KeyModel(-10, "", null, 0.5f, KeyType.SPACER),
            KeyModel('a'.code, "a", "@", 1.0f),
            KeyModel('s'.code, "s", "#", 1.0f),
            KeyModel('d'.code, "d", "$", 1.0f),
            KeyModel('f'.code, "f", "%", 1.0f),
            KeyModel('g'.code, "g", "&", 1.0f),
            KeyModel('h'.code, "h", "*", 1.0f),
            KeyModel('j'.code, "j", "-", 1.0f),
            KeyModel('k'.code, "k", "+", 1.0f),
            KeyModel('l'.code, "l", "/", 1.0f),
            KeyModel(-11, "", null, 0.5f, KeyType.SPACER)
        )

        val row3 = listOf(
            KeyModel(-1, "SHIFT", null, 1.3f, KeyType.SHIFT),
            KeyModel('z'.code, "z", "_", 1.0f),
            KeyModel('x'.code, "x", "\"", 1.0f),
            KeyModel('c'.code, "c", "'", 1.0f),
            KeyModel('v'.code, "v", ":", 1.0f),
            KeyModel('b'.code, "b", ";", 1.0f),
            KeyModel('n'.code, "n", "!", 1.0f),
            KeyModel('m'.code, "m", "?", 1.0f),
            KeyModel(-2, "DEL", null, 1.3f, KeyType.BACKSPACE)
        )

        val row4 = listOf(
            KeyModel(-3, "?123", null, 1.3f, KeyType.SYM),
            KeyModel(-20, "EMOJI", null, 1.0f, KeyType.EMOJI),
            KeyModel(','.code, ",", "<", 1.0f),
            KeyModel(32, "SPACE", null, 4.3f, KeyType.SPACE),
            KeyModel('.'.code, ".", ">", 1.0f),
            KeyModel(-6, "ENTER", null, 1.4f, KeyType.ACTION)
        )

        return listOf(row1, row2, row3, row4)
    }

    fun getSymbolsRows(): List<List<KeyModel>> {
        val row1 = listOf(
            KeyModel('1'.code, "1", "~", 1.0f),
            KeyModel('2'.code, "2", "`", 1.0f),
            KeyModel('3'.code, "3", "|", 1.0f),
            KeyModel('4'.code, "4", "•", 1.0f),
            KeyModel('5'.code, "5", "√", 1.0f),
            KeyModel('6'.code, "6", "π", 1.0f),
            KeyModel('7'.code, "7", "÷", 1.0f),
            KeyModel('8'.code, "8", "×", 1.0f),
            KeyModel('9'.code, "9", "¶", 1.0f),
            KeyModel('0'.code, "0", "°", 1.0f)
        )

        val row2 = listOf(
            KeyModel('@'.code, "@", null, 1.0f),
            KeyModel('#'.code, "#", null, 1.0f),
            KeyModel('$'.code, "$", "€", 1.0f),
            KeyModel('%'.code, "%", "¥", 1.0f),
            KeyModel('&'.code, "&", null, 1.0f),
            KeyModel('-'.code, "-", null, 1.0f),
            KeyModel('+'.code, "+", "±", 1.0f),
            KeyModel('('.code, "(", "[", 1.0f),
            KeyModel(')'.code, ")", "]", 1.0f),
            KeyModel('/'.code, "/", "\\", 1.0f)
        )

        val row3 = listOf(
            KeyModel(-8, "=\\<", null, 1.3f, KeyType.SYM_PAGE_2),
            KeyModel('*'.code, "*", "^", 1.0f),
            KeyModel('"'.code, "\"", "«", 1.0f),
            KeyModel('\''.code, "'", "»", 1.0f),
            KeyModel(':'.code, ":", null, 1.0f),
            KeyModel(';'.code, ";", null, 1.0f),
            KeyModel('!'.code, "!", "¡", 1.0f),
            KeyModel('?'.code, "?", "¿", 1.0f),
            KeyModel(-2, "DEL", null, 1.7f, KeyType.BACKSPACE)
        )

        val row4 = listOf(
            KeyModel(-7, "ABC", null, 1.3f, KeyType.SYM),
            KeyModel(-20, "EMOJI", null, 1.0f, KeyType.EMOJI),
            KeyModel('_'.code, "_", null, 1.0f),
            KeyModel(32, "SPACE", null, 4.3f, KeyType.SPACE),
            KeyModel('.'.code, ".", null, 1.0f),
            KeyModel(-6, "ENTER", null, 1.4f, KeyType.ACTION)
        )

        return listOf(row1, row2, row3, row4)
    }

    fun getSymbolsPage2Rows(): List<List<KeyModel>> {
        val row1 = listOf(
            KeyModel('~'.code, "~", null, 1.0f),
            KeyModel('\\'.code, "\\", null, 1.0f),
            KeyModel('|'.code, "|", null, 1.0f),
            KeyModel('<'.code, "<", null, 1.0f),
            KeyModel('>'.code, ">", null, 1.0f),
            KeyModel('{'.code, "{", null, 1.0f),
            KeyModel('}'.code, "}", null, 1.0f),
            KeyModel('['.code, "[", null, 1.0f),
            KeyModel(']'.code, "]", null, 1.0f),
            KeyModel('^'.code, "^", null, 1.0f)
        )

        val row2 = listOf(
            KeyModel('°'.code, "°", null, 1.0f),
            KeyModel('•'.code, "•", null, 1.0f),
            KeyModel('§'.code, "§", null, 1.0f),
            KeyModel('€'.code, "€", null, 1.0f),
            KeyModel('£'.code, "£", null, 1.0f),
            KeyModel('¥'.code, "¥", null, 1.0f),
            KeyModel('¢'.code, "¢", null, 1.0f),
            KeyModel('₹'.code, "₹", null, 1.0f),
            KeyModel('±'.code, "±", null, 1.0f),
            KeyModel('≠'.code, "≠", null, 1.0f)
        )

        val row3 = listOf(
            KeyModel(-3, "?123", null, 1.3f, KeyType.SYM_PAGE_2),
            KeyModel('÷'.code, "÷", null, 0.875f),
            KeyModel('×'.code, "×", null, 0.875f),
            KeyModel('¶'.code, "¶", null, 0.875f),
            KeyModel('©'.code, "©", null, 0.875f),
            KeyModel('®'.code, "®", null, 0.875f),
            KeyModel('™'.code, "™", null, 0.875f),
            KeyModel('¿'.code, "¿", null, 0.875f),
            KeyModel('¡'.code, "¡", null, 0.875f),
            KeyModel(-2, "DEL", null, 1.7f, KeyType.BACKSPACE)
        )

        val row4 = listOf(
            KeyModel(-7, "ABC", null, 1.3f, KeyType.SYM),
            KeyModel(-20, "EMOJI", null, 1.0f, KeyType.EMOJI),
            KeyModel('`'.code, "`", null, 1.0f),
            KeyModel(32, "SPACE", null, 4.3f, KeyType.SPACE),
            KeyModel('.'.code, ".", null, 1.0f),
            KeyModel(-6, "ENTER", null, 1.4f, KeyType.ACTION)
        )

        return listOf(row1, row2, row3, row4)
    }

    fun getTerminalNavRow(): List<KeyModel> {
        return listOf(
            KeyModel(111, "ESC", null, 1.05f),          // KeyEvent.KEYCODE_ESCAPE
            KeyModel(61, "TAB", null, 1.05f),           // KeyEvent.KEYCODE_TAB
            KeyModel(-30, "ALT", null, 1.05f),          // Toggle Alt Modifier
            KeyModel(21, "←", null, 0.95f),             // KeyEvent.KEYCODE_DPAD_LEFT
            KeyModel(20, "↓", null, 0.95f),             // KeyEvent.KEYCODE_DPAD_DOWN
            KeyModel(19, "↑", null, 0.95f),             // KeyEvent.KEYCODE_DPAD_UP
            KeyModel(22, "→", null, 0.95f),             // KeyEvent.KEYCODE_DPAD_RIGHT
            KeyModel(122, "HOME", null, 1.1f),          // KeyEvent.KEYCODE_MOVE_HOME
            KeyModel(123, "END", null, 1.0f),           // KeyEvent.KEYCODE_MOVE_END
            KeyModel(92, "PGUP", null, 1.1f),           // KeyEvent.KEYCODE_PAGE_UP
            KeyModel(93, "PGDN", null, 1.1f),           // KeyEvent.KEYCODE_PAGE_DOWN
            KeyModel(112, "DEL", null, 1.0f)            // KeyEvent.KEYCODE_FORWARD_DEL
        )
    }

    fun getFunctionKeysRow(): List<KeyModel> {
        return (1..12).map { fIndex ->
            KeyModel(
                code = 130 + fIndex, // KeyEvent.KEYCODE_F1 is 131
                primaryLabel = "F$fIndex",
                weight = 1.0f,
                type = KeyType.CHARACTER
            )
        }
    }

    fun getNumberRow(): List<KeyModel> {
        return listOf(
            KeyModel('1'.code, "1", null, 1.0f),
            KeyModel('2'.code, "2", null, 1.0f),
            KeyModel('3'.code, "3", null, 1.0f),
            KeyModel('4'.code, "4", null, 1.0f),
            KeyModel('5'.code, "5", null, 1.0f),
            KeyModel('6'.code, "6", null, 1.0f),
            KeyModel('7'.code, "7", null, 1.0f),
            KeyModel('8'.code, "8", null, 1.0f),
            KeyModel('9'.code, "9", null, 1.0f),
            KeyModel('0'.code, "0", null, 1.0f)
        )
    }

    fun getNumberPadRows(isDecimal: Boolean = true, isSigned: Boolean = false): List<List<KeyModel>> {
        val row1 = listOf(
            KeyModel('1'.code, "1", null, 1.0f),
            KeyModel('2'.code, "2", null, 1.0f),
            KeyModel('3'.code, "3", null, 1.0f),
            KeyModel(-2, "DEL", null, 1.0f, KeyType.BACKSPACE)
        )
        val row2 = listOf(
            KeyModel('4'.code, "4", null, 1.0f),
            KeyModel('5'.code, "5", null, 1.0f),
            KeyModel('6'.code, "6", null, 1.0f),
            KeyModel(if (isSigned) '-'.code else ':'.code, if (isSigned) "-" else ":", null, 1.0f)
        )
        val row3 = listOf(
            KeyModel('7'.code, "7", null, 1.0f),
            KeyModel('8'.code, "8", null, 1.0f),
            KeyModel('9'.code, "9", null, 1.0f),
            KeyModel(if (isDecimal) '.'.code else ','.code, if (isDecimal) "." else ",", null, 1.0f)
        )
        val row4 = listOf(
            KeyModel(-3, "ABC", null, 1.0f, KeyType.SYM),
            KeyModel('0'.code, "0", null, 1.0f),
            KeyModel(32, "SPACE", null, 1.0f, KeyType.SPACE),
            KeyModel(-6, "ENTER", null, 1.0f, KeyType.ACTION)
        )
        return listOf(row1, row2, row3, row4)
    }

    fun getPhonePadRows(): List<List<KeyModel>> {
        val row1 = listOf(
            KeyModel('1'.code, "1", null, 1.0f),
            KeyModel('2'.code, "2", null, 1.0f),
            KeyModel('3'.code, "3", null, 1.0f),
            KeyModel(-2, "DEL", null, 1.0f, KeyType.BACKSPACE)
        )
        val row2 = listOf(
            KeyModel('4'.code, "4", null, 1.0f),
            KeyModel('5'.code, "5", null, 1.0f),
            KeyModel('6'.code, "6", null, 1.0f),
            KeyModel('+'.code, "+", null, 1.0f)
        )
        val row3 = listOf(
            KeyModel('7'.code, "7", null, 1.0f),
            KeyModel('8'.code, "8", null, 1.0f),
            KeyModel('9'.code, "9", null, 1.0f),
            KeyModel('*'.code, "*", null, 1.0f)
        )
        val row4 = listOf(
            KeyModel(-3, "ABC", null, 1.0f, KeyType.SYM),
            KeyModel('0'.code, "0", null, 1.0f),
            KeyModel('#'.code, "#", null, 1.0f),
            KeyModel(-6, "ENTER", null, 1.0f, KeyType.ACTION)
        )
        return listOf(row1, row2, row3, row4)
    }
}
