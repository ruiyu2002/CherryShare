// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addMessage POST /api/message/add */
export async function addMessageUsingPost(body: API.AddMessage, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/message/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getTop500 POST /api/message/getTop500 */
export async function getTop500UsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseListMessageVO_>('/api/message/getTop500', {
    method: 'POST',
    ...(options || {}),
  })
}
