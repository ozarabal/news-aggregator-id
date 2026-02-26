import { getCategoryColors } from '../../utils/categoryColors'

interface Props {
  category: string
  size?: 'sm' | 'md'
}

export default function CategoryBadge({ category, size = 'sm' }: Props) {
  const colors = getCategoryColors(category)
  return (
    <span
      className={`
        inline-block font-body font-semibold uppercase tracking-widest border
        ${colors.bg} ${colors.text} ${colors.border}
        ${size === 'sm' ? 'text-[0.6rem] px-1.5 py-0.5' : 'text-xs px-2 py-1'}
      `}
    >
      {category}
    </span>
  )
}
