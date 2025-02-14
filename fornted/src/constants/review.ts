// 常用拒绝原因选项
export const REJECT_REASON_OPTIONS = [
  { label: '图片尺寸不符合要求', value: 'size_invalid' },
  { label: '图片内容不当', value: 'content_invalid' },
  { label: '图片质量过低', value: 'quality_low' },
  { label: '图片格式不支持', value: 'format_invalid' },
  { label: '图片包含敏感内容', value: 'sensitive_content' },
  { label: '图片可能侵犯版权', value: 'copyright_issue' },
  { label: '其他原因', value: 'other' },
]

// 拒绝原因对应的默认文案
export const REJECT_REASON_MAP = {
  size_invalid: '图片尺寸不符合平台要求，请上传符合要求的图片',
  content_invalid: '图片内容不符合平台规范，请重新上传',
  quality_low: '图片质量较低，建议上传清晰度更高的图片',
  format_invalid: '不支持该图片格式，请使用支持的格式重新上传',
  sensitive_content: '图片包含敏感内容，请遵守平台规范',
  copyright_issue: '图片可能存在版权问题，请确保拥有相关权限',
  other: '',
}

// 帖子拒绝原因选项
export const POST_REJECT_REASON_OPTIONS = [
  { label: '内容涉及敏感话题', value: 'sensitive_topic' },
  { label: '内容存在不当言论', value: 'inappropriate_content' },
  { label: '内容质量不符合要求', value: 'low_quality' },
  { label: '内容涉嫌抄袭/侵权', value: 'plagiarism' },
  { label: '内容包含垃圾广告', value: 'spam_ad' },
  { label: '内容不符合社区规范', value: 'violate_rules' },
  { label: '其他原因', value: 'other' },
]

// 帖子拒绝原因对应的默认文案
export const POST_REJECT_REASON_MAP = {
  sensitive_topic: '您的帖子涉及敏感话题，不适合在平台发布，请遵守相关法律法规。',
  inappropriate_content: '您的帖子包含不当言论，请文明发言，营造良好的社区氛围。',
  low_quality: '您的帖子内容质量不符合要求，建议补充更多有价值的信息后再发布。',
  plagiarism: '您的帖子内容涉嫌抄袭或侵犯他人权益，请确保发布原创内容。',
  spam_ad: '您的帖子包含垃圾广告信息，请勿发布与主题无关的广告内容。',
  violate_rules: '您的帖子不符合社区规范，请仔细阅读社区规则并遵守。',
  other: '',
}
