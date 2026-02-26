import { formatDistanceToNow, format, parseISO, isValid } from 'date-fns'
import { id } from 'date-fns/locale'

export function formatDate(dateString: string): string {
  try {
    const date = parseISO(dateString)
    if (!isValid(date)) return dateString

    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffHours = diffMs / (1000 * 60 * 60)

    if (diffHours < 24) {
      return formatDistanceToNow(date, { addSuffix: true, locale: id })
    }

    return format(date, 'd MMM yyyy, HH:mm', { locale: id })
  } catch {
    return dateString
  }
}

export function formatDateShort(dateString: string): string {
  try {
    const date = parseISO(dateString)
    if (!isValid(date)) return dateString
    return format(date, 'd MMM yyyy', { locale: id })
  } catch {
    return dateString
  }
}

export function truncate(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength).trimEnd() + '…'
}

export function formatCount(count: number): string {
  if (count >= 1_000_000) {
    return (count / 1_000_000).toFixed(1).replace('.0', '') + 'jt'
  }
  if (count >= 1_000) {
    return (count / 1_000).toFixed(1).replace('.0', '') + 'rb'
  }
  return count.toString()
}
