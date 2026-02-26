export function HeroSkeleton() {
  return (
    <div className="aspect-[16/9] bg-ink2 animate-pulse" />
  )
}

export function CardSkeleton() {
  return (
    <div className="border border-ink3 overflow-hidden animate-pulse">
      <div className="aspect-video bg-ink2" />
      <div className="p-4 space-y-2">
        <div className="h-3 w-16 bg-ink3" />
        <div className="h-4 bg-ink3 w-full" />
        <div className="h-4 bg-ink3 w-3/4" />
        <div className="h-3 bg-ink3 w-full" />
        <div className="h-3 bg-ink3 w-2/3" />
        <div className="pt-2 border-t border-ink3 flex gap-2">
          <div className="h-2 w-20 bg-ink3" />
          <div className="h-2 w-16 bg-ink3" />
        </div>
      </div>
    </div>
  )
}

export function SidebarCardSkeleton() {
  return (
    <div className="flex gap-3 py-3 border-b border-ink3 animate-pulse">
      <div className="w-20 h-14 bg-ink2 flex-shrink-0" />
      <div className="flex-1 space-y-2">
        <div className="h-3 bg-ink3 w-full" />
        <div className="h-3 bg-ink3 w-3/4" />
        <div className="h-2 w-24 bg-ink3" />
      </div>
    </div>
  )
}
