/**
 * 获取默认头像
 * @param userName 用户名
 * @returns 默认头像URL
 */
export const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(defaultName)}&backgroundColor=ffd5dc,ffdfbf,ffd5dc`
} 