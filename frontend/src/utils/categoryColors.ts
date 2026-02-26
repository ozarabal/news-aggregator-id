const categoryMap: Record<string, { bg: string; text: string; border: string }> = {
  teknologi: { bg: 'bg-blue-900/40', text: 'text-blue-300', border: 'border-blue-700' },
  bisnis: { bg: 'bg-emerald-900/40', text: 'text-emerald-300', border: 'border-emerald-700' },
  olahraga: { bg: 'bg-orange-900/40', text: 'text-orange-300', border: 'border-orange-700' },
  politik: { bg: 'bg-red-900/40', text: 'text-red-300', border: 'border-red-700' },
  hiburan: { bg: 'bg-purple-900/40', text: 'text-purple-300', border: 'border-purple-700' },
  kesehatan: { bg: 'bg-teal-900/40', text: 'text-teal-300', border: 'border-teal-700' },
  internasional: { bg: 'bg-cyan-900/40', text: 'text-cyan-300', border: 'border-cyan-700' },
}

const defaultColor = { bg: 'bg-ink3', text: 'text-muted', border: 'border-ink3' }

export function getCategoryColors(category: string) {
  return categoryMap[category.toLowerCase()] ?? defaultColor
}

export function getCategoryAccentColor(category: string): string {
  const colors: Record<string, string> = {
    teknologi: '#3b82f6',
    bisnis: '#10b981',
    olahraga: '#f97316',
    politik: '#ef4444',
    hiburan: '#a855f7',
    kesehatan: '#14b8a6',
    internasional: '#06b6d4',
  }
  return colors[category.toLowerCase()] ?? '#8a8070'
}
