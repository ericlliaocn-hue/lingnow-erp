import { ref, toRefs } from 'vue'
import useDictStore from '@/store/modules/dict'
import { getDicts } from '@/api/system/dict'

/**
 * 获取字典数据
 */
export function useDict(...args: string[]) {
  const res = ref<any>({})
  return (() => {
    args.forEach((dictType) => {
      res.value[dictType] = []
      const dictStore = useDictStore()
      const dicts = dictStore.getDict(dictType)
      if (dicts) {
        res.value[dictType] = dicts
      } else {
        getDicts(dictType).then((resp: any) => {
          res.value[dictType] = resp.map((p: any) => ({ 
            label: p.dictLabel, 
            value: p.dictValue, 
            elTagType: p.listClass, 
            class: p.cssClass 
          }))
          dictStore.setDict(dictType, res.value[dictType])
        })
      }
    })
    return toRefs(res.value)
  })()
}
