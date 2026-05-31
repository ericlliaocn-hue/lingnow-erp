import { defineStore } from 'pinia'

export const useDictStore = defineStore('dict', {
  state: () => ({
    dict: new Array<any>()
  }),
  actions: {
    // 获取字典
    getDict(key: string) {
      if (key == null && key == "") {
        return null
      }
      try {
        for (let i = 0; i < this.dict.length; i++) {
          if (this.dict[i].key == key) {
            return this.dict[i].value
          }
        }
      } catch (e) {
        return null
      }
    },
    // 设置字典
    setDict(key: string, value: any) {
      if (key !== null && key !== "") {
        this.dict.push({
          key: key,
          value: value
        })
      }
    },
    // 删除字典
    removeDict(key: string) {
      try {
        for (let i = 0; i < this.dict.length; i++) {
          if (this.dict[i].key == key) {
            this.dict.splice(i, 1)
            return
          }
        }
      } catch (e) {}
    },
    // 清空字典
    cleanDict() {
      this.dict = new Array<any>()
    }
  }
})

export default useDictStore
