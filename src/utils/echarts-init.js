/**
 * ECharts 按需引入（tree-shaking 优化）
 *
 * 本模块替代 ECharts 的全量导入（import * as echarts from 'echarts'），
 * 仅注册项目实际使用的图表类型与组件，将打包体积从约 1MB 降至约 350KB。
 *
 * 当前注册的图表类型：
 * - LineChart：折线图（学习趋势、番茄趋势）
 * - BarChart：柱状图（任务统计、小时分布）
 * - PieChart：环形图（课程分布、状态占比）
 * - HeatmapChart：热力图（学习热力图）
 *
 * 当前注册的组件：
 * - CanvasRenderer：Canvas 渲染器
 * - TitleComponent：标题组件
 * - TooltipComponent：提示框组件
 * - LegendComponent：图例组件
 * - GridComponent：网格/直角坐标系组件
 * - VisualMapComponent：视觉映射组件（热力图色阶）
 * - CalendarComponent：日历坐标系组件（热力图日期轴）
 *
 * 使用方式：
 *   import echarts from '@/utils/echarts-init'
 *   // 直接使用 echarts.init() / echarts 的所有已注册功能
 *   替代原先的 import * as echarts from 'echarts' 或 import('echarts')
 *
 * 如需新增图表类型（如 ScatterChart），在此文件添加导入并注册即可。
 *
 * @module utils/echarts-init
 */
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart, HeatmapChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  VisualMapComponent,
  CalendarComponent,
} from 'echarts/components'

// 注册所需的图表类型和组件（按需引入，减少打包体积）
echarts.use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  HeatmapChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  VisualMapComponent,
  CalendarComponent,
])

export default echarts
