import { create } from 'zustand'

interface AppStore {
  selectedCategory: string | null
  setSelectedCategory: (cat: string | null) => void
  searchQuery: string
  setSearchQuery: (q: string) => void
  sourceModalOpen: boolean
  editingSourceId: number | null
  openSourceModal: (id?: number) => void
  closeSourceModal: () => void
  deleteConfirmId: number | null
  openDeleteConfirm: (id: number) => void
  closeDeleteConfirm: () => void
}

export const useAppStore = create<AppStore>((set) => ({
  selectedCategory: null,
  setSelectedCategory: (cat) => set({ selectedCategory: cat }),
  searchQuery: '',
  setSearchQuery: (q) => set({ searchQuery: q }),
  sourceModalOpen: false,
  editingSourceId: null,
  openSourceModal: (id) => set({ sourceModalOpen: true, editingSourceId: id ?? null }),
  closeSourceModal: () => set({ sourceModalOpen: false, editingSourceId: null }),
  deleteConfirmId: null,
  openDeleteConfirm: (id) => set({ deleteConfirmId: id }),
  closeDeleteConfirm: () => set({ deleteConfirmId: null }),
}))
