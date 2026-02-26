// =============================================
// ARTICLE
// =============================================

export interface ArticleSummary {
  id: number
  title: string
  url: string
  description: string | null
  thumbnailUrl: string | null
  author: string | null
  category: string
  sourceName: string
  sourceId: number
  publishedAt: string
  viewCount: number
}

export interface ArticleDetail {
  id: number
  title: string
  url: string
  description: string | null
  content: string | null
  thumbnailUrl: string | null
  author: string | null
  category: string
  sourceName: string
  sourceWebsiteUrl: string | null
  sourceId: number
  publishedAt: string
  viewCount: number
  isScraped: boolean
  scraped?: boolean // Lombok serialization fallback
  createdAt: string
}

// =============================================
// SOURCE
// =============================================

export interface Source {
  id: number
  name: string
  url: string
  websiteUrl: string | null
  category: string
  isActive: boolean
  lastCrawledAt: string | null
  crawlStatus: 'PENDING' | 'SUCCESS' | 'ERROR' | null
  createdAt: string
}

export interface SourceRequest {
  name: string
  url: string
  websiteUrl?: string
  category: string
  isActive?: boolean
}

// =============================================
// PAGINATION
// =============================================

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
}

// =============================================
// API WRAPPER
// =============================================

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

// =============================================
// CRAWLER
// =============================================

export interface CrawlResult {
  status: 'SUCCESS' | 'FAILED'
  sourceName: string
  articlesFound: number
  articlesSaved: number
  durationMs: number
  errorMessage?: string
}

export interface CrawlStats {
  articlesSavedToday: number
  totalActiveSources: number
}

// =============================================
// CACHE
// =============================================

export interface CacheInfo {
  keyCount: number
  keys: string[]
}

export type CacheStats = Record<string, CacheInfo>

// =============================================
// DIGEST
// =============================================

export interface DigestStats {
  sentToday: number
  failedToday: number
  totalUsers: number
}

// =============================================
// AUTH / USER
// =============================================

export interface UserProfile {
  id: number
  email: string
  fullName: string
  digestEnabled: boolean
  digestFrequency: 'DAILY' | 'WEEKLY'
  categories: string[]
  role: 'ADMIN' | 'USER'
}

export interface AuthResponse {
  token: string
  userId: number
  email: string
  fullName: string
  digestEnabled: boolean
  digestFrequency: 'DAILY' | 'WEEKLY'
  categories: string[]
  role: string
}
