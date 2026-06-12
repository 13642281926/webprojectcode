<script setup>
/**
 * 数据统计卡片
 * @props label - 指标名称
 * @props value - 指标数值
 * @props unit - 单位后缀
 * @props icon - Element Plus 图标组件
 * @props color - 图标背景色
 * @props clickable - 是否可点击（hover 增强）
 * @emit click - 卡片点击（clickable 为 true 时）
 * @slot default - 自定义数值区域
 * @slot footer - 底部扩展信息
 */
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: '--' },
  unit: { type: String, default: '' },
  icon: { type: Object, default: null },
  color: { type: String, default: '#3b82f6' },
  clickable: { type: Boolean, default: false },
})

const emit = defineEmits(['click'])

/**
 * 卡片点击事件代理
 * 仅当 clickable 为 true 时 emit('click')
 */
function handleClick() {
  emit('click')
}
</script>

<template>
  <!-- 统计卡片容器：毛玻璃 + 底部光晕 + hover 上浮效果 -->
  <el-card
    class="stat-card glass-card glass-card--glow hover-lift"
    :class="{ 'stat-card--clickable': clickable }"
    shadow="never"
    @click="clickable && handleClick()"
  >
    <div class="stat-card__body">
      <!--
        图标区域（v-if="icon"：无图标时不渲染此区域）
        背景色 = color + "22"（约 13% 透明度，十六进制拼接）
        前景色 = color（纯色）
      -->
      <div
        v-if="icon"
        class="stat-card__icon"
        :style="{ background: `${color}22`, color }"
      >
        <el-icon :size="24"><component :is="icon" /></el-icon>
      </div>

      <!-- 内容区域 -->
      <div class="stat-card__content">
        <!-- 指标名称 -->
        <p class="stat-card__label">{{ label }}</p>

        <!--
          数值区域（默认插槽）
          默认渲染：大号渐变文字（value + 可选 unit 后缀）
          父组件可通过 #default 插槽覆盖自定义显示
        -->
        <slot>
          <p class="stat-card__value gradient-text">
            {{ value }}<span v-if="unit" class="stat-card__unit">{{ unit }}</span>
          </p>
        </slot>

        <!-- 底部扩展插槽（如显示趋势箭头、同比数据等） -->
        <slot name="footer" />
      </div>
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.stat-card {
  height: 100%;
}

.stat-card--clickable {
  cursor: pointer;
}

.stat-card__body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card__icon {
  width: 52px;
  height: 52px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform $transition-fast;
}

.stat-card--clickable:hover .stat-card__icon {
  transform: scale(1.08);
}

.stat-card__label {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.stat-card__value {
  font-size: 32px;
  font-weight: 700;
  margin-top: 4px;
  line-height: 1.2;
}

.stat-card__unit {
  font-size: 16px;
  margin-left: 2px;
}
</style>
