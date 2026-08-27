import type { ShopCategory } from '@/types/shop'

export interface ShopCategoryNode extends ShopCategory {
  children: ShopCategoryNode[]
}

export function buildCategoryTree(records: ShopCategory[]): ShopCategoryNode[] {
  const nodes = new Map<string, ShopCategoryNode>()
  records.forEach(record => nodes.set(String(record.id), { ...record, id: String(record.id), children: [] }))

  const roots: ShopCategoryNode[] = []
  nodes.forEach(node => {
    const parentId = String(node.parentId || '0')
    const parent = nodes.get(parentId)
    if (parent && parent.id !== node.id) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })

  const sort = (items: ShopCategoryNode[]) => {
    items.sort((left, right) => Number(left.sortOrder || 0) - Number(right.sortOrder || 0))
    items.forEach(item => sort(item.children))
  }
  sort(roots)
  return roots
}

export function descendantCategoryIds(records: ShopCategory[], rootId: string) {
  const ids = new Set<string>([String(rootId)])
  let changed = true
  while (changed) {
    changed = false
    records.forEach(record => {
      const id = String(record.id)
      const parentId = String(record.parentId || '0')
      if (ids.has(parentId) && !ids.has(id)) {
        ids.add(id)
        changed = true
      }
    })
  }
  return ids
}

export function categoryPath(records: ShopCategory[], categoryId?: string) {
  const byId = new Map(records.map(item => [String(item.id), item]))
  const path: ShopCategory[] = []
  const visited = new Set<string>()
  let current = categoryId ? byId.get(String(categoryId)) : undefined
  while (current && !visited.has(String(current.id))) {
    visited.add(String(current.id))
    path.unshift(current)
    current = byId.get(String(current.parentId || '0'))
  }
  return path
}
