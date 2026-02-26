import { FileX } from 'lucide-react'

interface Props {
  title?: string
  description?: string
}

export default function EmptyState({
  title = 'Tidak ada artikel',
  description = 'Belum ada artikel yang ditemukan.',
}: Props) {
  return (
    <div className="flex flex-col items-center justify-center py-20 px-4 text-center">
      <div className="border border-ink3 p-6 mb-4">
        <FileX size={32} className="text-muted" />
      </div>
      <h3 className="font-display text-display-sm text-paper mb-2">{title}</h3>
      <p className="text-muted text-sm font-body max-w-sm">{description}</p>
    </div>
  )
}
