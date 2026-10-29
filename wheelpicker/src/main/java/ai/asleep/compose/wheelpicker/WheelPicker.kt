package ai.asleep.compose.wheelpicker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 원통형으로 굴러가듯 스크롤해서 값 하나를 선택하는 범용 Wheel Picker.
 *
 * [itemHeight]를 넘기면 그 값을 그대로 쓰고, 넘기지 않으면 부모 컨테이너 높이를 [visibleItemCount]로
 * 나눠서 자동 계산한다. 이 경우 부모는 유한한 높이 제약을 줘야 한다.
 *
 * @param items 휠에 표시할 문자열 목록.
 * @param initialIndex 초기 선택 인덱스 (0-based).
 * @param onValueChange 스크롤이 멈춘 후 선택된 값으로 호출되는 콜백. (index: Int, value: String)
 * @param modifier Modifier.
 * @param visibleItemCount 중앙 영역에 완전히 노출되는 아이템 개수 (중앙 정렬을 위해 홀수만 허용).
 *   권장 범위는 3..7. 그보다 크면 가장자리 아이템이 거의 옆으로 누워서 읽기 힘들어진다.
 * @param peekItemCount [visibleItemCount] 양옆에 부분적으로 비치며 fade-out되는 아이템 개수.
 *   0이면 visible 끝에서 빠르게 cutoff. 권장 범위는 1..3이며,
 *   [visibleItemCount] + [peekItemCount] ≤ 10 정도로 유지하는 게 곡면 느낌이 가장 자연스럽다.
 * @param itemHeight 각 아이템의 높이. null이면 부모 컨테이너 높이를 기준으로 자동 계산.
 * @param repeatable true면 무한 스크롤(끝나면 처음으로).
 * @param textStyle 아이템 텍스트 스타일.
 * @param textColor 아이템 텍스트 컬러 (중앙 기준, 가장자리로 갈수록 투명해짐).
 *   [textStyle]의 color보다 우선한다.
 * @param cylindrical true면 원통형 3D 변환을 적용한다.
 * @param fadeEdges true면 위/아래 가장자리를 그라데이션으로 페이드아웃한다.
 */
@Composable
public fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onValueChange: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 3,
    peekItemCount: Int = 1,
    itemHeight: Dp? = null,
    repeatable: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.headlineLarge,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    cylindrical: Boolean = true,
    fadeEdges: Boolean = true,
) {
    require(visibleItemCount % 2 == 1) {
        "visibleItemCount must be odd, but was $visibleItemCount"
    }

    // In repeatable mode, start near the middle of Int range so the user can scroll
    // in either direction "forever" without hitting the edge. Snap to an items.size
    // boundary first so (page % items.size) == initialIndex.
    val initialPage = remember(items.size, initialIndex, repeatable) {
        if (repeatable) {
            val halfway = Int.MAX_VALUE / 2
            halfway - (halfway % items.size) + initialIndex
        } else {
            initialIndex
        }
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { if (repeatable) Int.MAX_VALUE else items.size }
    )

    // Keyed on pagerState only, so this coroutine runs for the lifetime of the picker;
    // rememberUpdatedState lets it read the *latest* callback / items without restarting.
    // drop(1) skips snapshotFlow's initial emit (current value on subscribe, not a user action).
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentItems by rememberUpdatedState(items)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .drop(1)
            .collect { settled ->
                val snapshot = currentItems
                if (snapshot.isEmpty()) return@collect
                // In repeatable mode settled can be any large Int — map back to a real index.
                val index = (settled % snapshot.size).coerceAtMost(snapshot.lastIndex)
                currentOnValueChange(index, snapshot[index])
            }
    }

    val localDensity = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Container is itemHeight * ratio, where ratio accounts for cylindrical curvature
        // pulling outer items inward — avoids empty top/bottom space for larger vIC.
        val containerRatio = wheelContainerRatio(visibleItemCount, peekItemCount, cylindrical)
        // Resolution priority: explicit itemHeight → parent height / containerRatio
        // → fall back to a size derived from textStyle (when parent height is unbounded).
        val resolvedItemHeight = itemHeight ?: when {
            constraints.hasBoundedHeight && maxHeight > 0.dp -> maxHeight / containerRatio
            else -> deriveItemHeight(textStyle, localDensity)
        }
        val containerHeight = resolvedItemHeight * containerRatio
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight)
                .then(
                    // DstIn blending needs an offscreen compositing layer so the gradient
                    // multiplies against an already-composited pager, not the screen buffer.
                    if (fadeEdges) Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.5f to Color.Black,
                                    1f to Color.Transparent,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                    else Modifier
                ),
            // Vertical padding so the selected page sits at the container's vertical center.
            contentPadding = PaddingValues(
                vertical = (containerHeight - resolvedItemHeight) / 2f
            ),
            pageSize = PageSize.Fixed(resolvedItemHeight),
            beyondViewportPageCount = peekItemCount,
            // Low threshold (0.25) so a light flick still advances one item; snap distance
            // cap keeps very fast flings from skipping wildly past the intended target.
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(100),
                snapPositionalThreshold = 0.25f
            )
        ) { index ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (cylindrical) Modifier.graphicsLayer {
                            // Each item is rotated around its own X axis AND translated
                            // along the cylinder's Y curve so adjacent items abut as
                            // closely as possible to a continuous surface.
                            // The outermost rendered item (visible edge + peek) is pinned
                            // to MAX_OUTER_ANGLE_DEG, and the per-slot angle is derived
                            // from that — so the curve arc stays consistent regardless of
                            // visibleItemCount or peekItemCount.
                            val maxOffset = (visibleItemCount - 1) / 2f
                            val outerOffset = maxOffset + peekItemCount
                            val anglePerSlotDeg = MAX_OUTER_ANGLE_DEG / outerOffset
                            val anglePerSlotRad = anglePerSlotDeg * RAD_PER_DEG

                            val pageOffset = (index - pagerState.currentPage) -
                                    pagerState.currentPageOffsetFraction
                            val angleDeg = (pageOffset * anglePerSlotDeg)
                                .coerceIn(-MAX_OUTER_ANGLE_DEG, MAX_OUTER_ANGLE_DEG)
                            val angleRad = angleDeg * RAD_PER_DEG

                            // Cylinder radius calibrated so adjacent items are exactly
                            // itemHeight apart at the center. This keeps spacing uniform
                            // across different visibleItemCount values; peek items land
                            // wherever geometry places them (and fade out via alpha).
                            val itemHeightPx = size.height
                            val radiusPx = itemHeightPx / sin(anglePerSlotRad)

                            translationY = radiusPx * sin(angleRad) - pageOffset * itemHeightPx
                            rotationX = -angleDeg

                            // Linear fade across the peek range; items past peek edge are hidden.
                            val fadeRange = peekItemCount.toFloat().coerceAtLeast(0.25f)
                            alpha =
                                (1f - (abs(pageOffset) - maxOffset) / fadeRange).coerceIn(0f, 1f)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index % items.size],
                    color = textColor,
                    style = textStyle
                )
            }
        }
    }
}

// Angle at which the outermost rendered item (visible edge + peek) is drawn.
// Kept under 90° so the edge item isn't rotated fully perpendicular (edge-on = invisible).
private const val MAX_OUTER_ANGLE_DEG = 90f
private const val RAD_PER_DEG: Float = (PI / 180.0).toFloat()

/**
 * Ratio of the wheel container height to a single item's height, given cylindrical curvature.
 *
 * In cylindrical mode, the outermost visible item is pulled toward the center by the curve,
 * so a naive `visibleItemCount * itemHeight` container leaves empty space at top/bottom.
 * This returns the exact ratio needed to make the container bound the outermost visible
 * item's top/bottom edges. In non-cylindrical mode this collapses to [visibleItemCount].
 */
internal fun wheelContainerRatio(
    visibleItemCount: Int,
    peekItemCount: Int,
    cylindrical: Boolean,
): Float {
    if (!cylindrical) return visibleItemCount.toFloat()
    val maxOffset = (visibleItemCount - 1) / 2f
    if (maxOffset == 0f) return 1f
    val outerOffset = maxOffset + peekItemCount
    val anglePerSlotRad = (MAX_OUTER_ANGLE_DEG * RAD_PER_DEG) / outerOffset
    val edgeAngleRad = maxOffset * anglePerSlotRad
    // Edge item's center position (from pager center) in units of itemHeight.
    val edgeCenter = sin(edgeAngleRad) / sin(anglePerSlotRad)
    return 2f * edgeCenter + 1f
}

/**
 * Derive a sensible item height from the given [textStyle].
 * Uses lineHeight if specified, falls back to fontSize × 1.2, and adds 16dp of vertical padding.
 */
internal fun deriveItemHeight(textStyle: TextStyle, density: Density): Dp = with(density) {
    val lineHeight = textStyle.lineHeight
    val fontSize = textStyle.fontSize
    val pxHeight = when {
        lineHeight.type == TextUnitType.Sp -> lineHeight.toPx()
        fontSize.type == TextUnitType.Sp -> fontSize.toPx() * 1.2f
        else -> 48.dp.toPx()
    }
    pxHeight.toDp() + 16.dp
}

@Preview(showBackground = true, backgroundColor = 0xFFF, name = "vIC=3")
@Composable
private fun WheelPickerPreviewVIC3() {
    MaterialTheme {
        WheelPicker(
            items = (0..12).map { "$it" },
            initialIndex = 6,
            onValueChange = { _, _ -> },
//            modifier = Modifier.height(240.dp),
            visibleItemCount = 3,
            repeatable = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF, name = "vIC=5")
@Composable
private fun WheelPickerPreviewVIC5() {
    MaterialTheme {
        WheelPicker(
            items = (0..12).map { "$it" },
            initialIndex = 6,
            onValueChange = { _, _ -> },
//            modifier = Modifier.height(240.dp),
            visibleItemCount = 5,
            repeatable = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF, name = "vIC=7")
@Composable
private fun WheelPickerPreviewVIC7() {
    MaterialTheme {
        WheelPicker(
            items = (0..12).map { "$it" },
            initialIndex = 6,
            onValueChange = { _, _ -> },
//            modifier = Modifier.height(240.dp),
            visibleItemCount = 7,
            repeatable = true,
        )
    }
}