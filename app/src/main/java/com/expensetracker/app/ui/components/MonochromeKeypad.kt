package com.expensetracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonochromeKeypad(
    modifier: Modifier = Modifier,
    onDigitClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onEqualsClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Clear, Division, Multiplication, Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton(
                text = "C",
                modifier = Modifier.weight(1f),
                isSecondary = true,
                onClick = onClearClick
            )
            KeypadButton(
                text = "÷",
                modifier = Modifier.weight(1f),
                isSecondary = true,
                onClick = { onOperatorClick("÷") }
            )
            KeypadButton(
                text = "×",
                modifier = Modifier.weight(1f),
                isSecondary = true,
                onClick = { onOperatorClick("×") }
            )
            KeypadButton(
                text = "⌫",
                modifier = Modifier.weight(1f),
                isSecondary = true,
                onClick = onBackspaceClick
            )
        }

        // Row 2: 7, 8, 9, Minus
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton(text = "7", modifier = Modifier.weight(1f), onClick = { onDigitClick("7") })
            KeypadButton(text = "8", modifier = Modifier.weight(1f), onClick = { onDigitClick("8") })
            KeypadButton(text = "9", modifier = Modifier.weight(1f), onClick = { onDigitClick("9") })
            KeypadButton(
                text = "−",
                modifier = Modifier.weight(1f),
                isSecondary = true,
                onClick = { onOperatorClick("-") }
            )
        }

        // Row 3: 4, 5, 6, Plus
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton(text = "4", modifier = Modifier.weight(1f), onClick = { onDigitClick("4") })
            KeypadButton(text = "5", modifier = Modifier.weight(1f), onClick = { onDigitClick("5") })
            KeypadButton(text = "6", modifier = Modifier.weight(1f), onClick = { onDigitClick("6") })
            KeypadButton(
                text = "+",
                modifier = Modifier.weight(1f),
                isSecondary = true,
                onClick = { onOperatorClick("+") }
            )
        }

        // Row 4: 1, 2, 3, Equals
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton(text = "1", modifier = Modifier.weight(1f), onClick = { onDigitClick("1") })
            KeypadButton(text = "2", modifier = Modifier.weight(1f), onClick = { onDigitClick("2") })
            KeypadButton(text = "3", modifier = Modifier.weight(1f), onClick = { onDigitClick("3") })
            KeypadButton(
                text = "=",
                modifier = Modifier.weight(1f),
                isPrimaryAction = true,
                onClick = onEqualsClick
            )
        }

        // Row 5: 0, 00, Decimal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton(
                text = "0",
                modifier = Modifier.weight(2f),
                onClick = { onDigitClick("0") }
            )
            KeypadButton(
                text = "00",
                modifier = Modifier.weight(1f),
                onClick = { onDigitClick("00") }
            )
            KeypadButton(
                text = ".",
                modifier = Modifier.weight(1f),
                onClick = onDecimalClick
            )
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    isPrimaryAction: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isPrimaryAction -> MaterialTheme.colorScheme.primary
        isSecondary -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isPrimaryAction -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = if (isPrimaryAction || isSecondary) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}
