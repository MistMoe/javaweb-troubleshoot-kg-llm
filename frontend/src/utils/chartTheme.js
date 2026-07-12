export const chartPalette = ['#4f86e8', '#9689e6', '#62b9ca', '#68b99a', '#dc7378']

export const chartAxisStyle = {
  axisLine: {
    lineStyle: {
      color: 'rgba(139, 164, 191, 0.34)'
    }
  },
  axisTick: { show: false },
  axisLabel: {
    color: '#7f91a6',
    fontSize: 11
  },
  splitLine: {
    lineStyle: {
      color: 'rgba(139, 164, 191, 0.16)'
    }
  }
}

export const chartTooltipStyle = {
  backgroundColor: 'rgba(246, 249, 252, 0.98)',
  borderColor: 'rgba(139, 164, 191, 0.34)',
  borderWidth: 1,
  padding: [9, 11],
  textStyle: {
    color: '#17263d',
    fontSize: 12
  },
  extraCssText: 'box-shadow: 8px 10px 22px rgba(105,132,160,.16); border-radius: 6px;'
}

export function createChartResizeObserver(element, onResize) {
  if (!element || typeof ResizeObserver === 'undefined') return () => {}

  const observer = new ResizeObserver(() => onResize())
  observer.observe(element)
  return () => observer.disconnect()
}
