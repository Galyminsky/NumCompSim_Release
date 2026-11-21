package me.proton.jobforandroid.compositionofnumbers.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import me.proton.jobforandroid.compositionofnumbers.R
import kotlin.math.min

/** Displays supplied equal-part counts only; no question generation or answer checking. */
class FractionPartsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val partWidth = resources.getDimension(R.dimen.ncs_v2_fraction_part_width)
    private val partHeight = resources.getDimension(R.dimen.ncs_v2_fraction_part_height)
    private val gap = resources.getDimension(R.dimen.ncs_v2_fraction_part_gap)
    private val radius = resources.getDimension(R.dimen.ncs_v2_fraction_part_radius)
    private val stroke = resources.getDimension(R.dimen.ncs_v2_fraction_part_stroke)
    private val shadedColor = ContextCompat.getColor(context, R.color.ncs_v2_yellow)
    private val emptyColor = ContextCompat.getColor(context, R.color.ncs_v2_surface)
    private val borderColor = ContextCompat.getColor(context, R.color.ncs_v2_border)
    private var total = 0
    private var shaded = 0

    init {
        // Approved picture example is confined to Android Studio Layout Preview.
        if (isInEditMode) {
            total = 4
            shaded = 1
        }
    }

    fun setParts(totalParts: Int, shadedParts: Int) {
        require(totalParts >= 0 && shadedParts in 0..totalParts)
        if (total == totalParts && shaded == shadedParts) return
        total = totalParts
        shaded = shadedParts
        contentDescription = if (total == 0) null else context.getString(
            R.string.ncs_v2_fraction_parts_accessibility, shaded, total)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = if (total == 0) 0f else total * partWidth + (total - 1) * gap
        setMeasuredDimension(resolveSize(maxOf(suggestedMinimumWidth, (width + paddingLeft + paddingRight).toInt()), widthMeasureSpec),
            resolveSize(maxOf(suggestedMinimumHeight, (partHeight + paddingTop + paddingBottom).toInt()), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (total == 0) return
        val availableWidth = (width - paddingLeft - paddingRight).toFloat()
        val availableHeight = (height - paddingTop - paddingBottom).toFloat()
        if (availableWidth <= 0 || availableHeight <= 0) return
        val groupWidth = total * partWidth + (total - 1) * gap
        val scale = min(1f, min(availableWidth / groupWidth, availableHeight / partHeight))
        val saved = canvas.save()
        canvas.translate(paddingLeft + (availableWidth - groupWidth * scale) / 2,
            paddingTop + (availableHeight - partHeight * scale) / 2)
        canvas.scale(scale, scale)
        repeat(total) { index ->
            val left = index * (partWidth + gap)
            paint.style = Paint.Style.FILL
            paint.color = if (index < shaded) shadedColor else emptyColor
            canvas.drawRoundRect(left, 0f, left + partWidth, partHeight, radius, radius, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            paint.color = borderColor
            canvas.drawRoundRect(left + stroke / 2, stroke / 2, left + partWidth - stroke / 2,
                partHeight - stroke / 2, radius - stroke / 2, radius - stroke / 2, paint)
        }
        canvas.restoreToCount(saved)
    }

    companion object {
        @JvmStatic
        @BindingAdapter(value = ["totalParts", "shadedParts"], requireAll = true)
        fun bindParts(view: FractionPartsView, totalParts: Int, shadedParts: Int) {
            view.setParts(totalParts, shadedParts)
        }
    }
}
