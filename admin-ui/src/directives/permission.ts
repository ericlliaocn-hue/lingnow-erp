import type { DirectiveBinding } from 'vue'

/**
 * 权限指令
 * 用法：在按钮或操作入口上绑定权限标识。
 */
export const permission = {
    mounted(el: HTMLElement, binding: DirectiveBinding) {
        const { value } = binding
        const permissions = JSON.parse(localStorage.getItem('permissions') || '[]')

        if (value && value instanceof Array && value.length > 0) {
            const permissionRoles = value
            const hasPermission = permissions.some((perm: string) => {
                return permissionRoles.includes(perm) || perm === '*:*:*'
            })

            if (!hasPermission) {
                el.parentNode && el.parentNode.removeChild(el)
            }
        } else if (value && typeof value === 'string') {
            const hasPermission = permissions.includes(value) || permissions.includes('*:*:*')
            if (!hasPermission) {
                el.parentNode && el.parentNode.removeChild(el)
            }
        } else {
            throw new Error('need permissions! Like v-permission="\'sys:sysUser:add\'"')
        }
    }
}
