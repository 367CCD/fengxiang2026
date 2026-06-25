# 接口文档 — 风享（仿小红书博客系统）

> **基础路径**: `/api`  
> **后端地址**: `http://localhost:8081`（Vite 代理 `/api` → `http://localhost:8081`，并去掉 `/api` 前缀）  
> **统一返回格式**:
> ```json
> { "code": 1, "msg": "成功", "data": null }
> ```
> - `code=1` → 成功，前端收到 `res.data`
> - `code=0` → 失败，前端显示 `res.msg`
> **鉴权方式**: 请求头 `token: xxx`（登录后返回的 JWT）  
> **字段命名**: 全部使用 **camelCase（驼峰）**，对应数据库 `snake_case`

---

# 一、用户模块

## 1.1 用户注册
- **功能**: 新用户注册
- **路径**: `POST /register`
- **无需 Token**

```json
// Request
{
  "username": "newuser",       // 必填, 2~20字符
  "passwordHash": "123456",    // 必填, 6~16字符
  "phone": "13800138000",      // 选填
  "email": "user@example.com", // 选填
  "captcha": "A1B2"            // 必填, 验证码
}

// Response data → ["username", "token"]
// 例: ["newuser", "eyJhbGciOiJIUzI1NiJ9..."]
```

## 1.2 用户登录
- **功能**: 用户名+密码+验证码登录
- **路径**: `POST /login`
- **无需 Token**

```json
// Request
{
  "username": "admin",
  "passwordHash": "admin123",
  "captcha": "A1B2"
}

// Response data → ["username", "token"]
// 前端收到后分别存 localStorage: token / user
```

## 1.3 获取验证码图片
- **功能**: 生成验证码 PNG 图片，session 存答案
- **路径**: `GET /captcha`
- **无需 Token**
- **响应**: `Content-Type: image/png`（二进制流）
- **前端使用**: `<img :src="'/captcha?t=' + timestamp" @click="refreshCaptcha">`

## 1.4 获取用户信息
- **功能**: 获取指定用户或当前登录用户信息
- **路径**: `GET /users/{id}` 或 `GET /users/`（当前用户）
- **需要 Token**

```json
// Response data
{
  "id": 1,
  "username": "admin",
  "phone": "13800138000",
  "email": "admin@example.com",
  "avatarUrl": "https://oss.example.com/avatars/1.jpg",
  "bio": "个人简介",
  "gender": 0,          // 0未知 1男 2女
  "location": "北京",
  "birthday": "2000-01-01",
  "status": 1,          // 0禁用 1正常
  "role": 0,            // 0普通 1管理员
  "createdAt": "2026-06-10T12:00:00"
}
```

## 1.5 更新用户资料
- **功能**: 修改头像、简介、性别、地区等
- **路径**: `PUT /users/profile`
- **需要 Token**

```json
// Request
{
  "avatarUrl": "https://...",
  "bio": "新简介",
  "gender": 1,
  "location": "上海"
}

// Response data: null
```

## 1.6 关注用户
- **功能**: 关注某个用户
- **路径**: `POST /users/{userId}/follow`
- **需要 Token**
- **Response data**: `null`

## 1.7 取消关注
- **功能**: 取消关注某个用户
- **路径**: `DELETE /users/{userId}/follow`
- **需要 Token**
- **Response data**: `null`

## 1.8 关注者列表
- **功能**: 查看某个用户的粉丝
- **路径**: `GET /users/{userId}/followers`
- **需要 Token**

```json
// Query Params: page=1, pageSize=20

// Response data
{
  "list": [
    {
      "id": 2,
      "username": "user2",
      "avatarUrl": "https://...",
      "bio": "..."
    }
  ],
  "total": 10,
  "page": 1,
  "pageSize": 20
}
```

## 1.9 关注列表
- **功能**: 查看某个用户关注的人
- **路径**: `GET /users/{userId}/following`
- **需要 Token**
- **Response data**: 同 1.8

---

# 二、帖子模块

## 2.1 瀑布流帖子列表（发现页）
- **功能**: 发现页瀑布流展示，支持排序、标签筛选、关键词搜索
- **路径**: `GET /posts`
- **需要 Token**

```json
// Query Params
// page    | int    | 默认 1
// pageSize| int    | 默认 20
// sort    | string | "latest"(最新,默认) / "hot"(最热)
// tagId   | long   | 按标签筛选, 可选
// q       | string | 关键词全文搜索, 可选

// Response data
{
  "list": [
    {
      "id": 1,
      "title": "今天的穿搭分享",
      "coverUrl": "https://oss.example.com/cover/1.jpg",
      "type": 1,                     // 1=图文 2=视频
      "likeCount": 128,
      "commentCount": 35,
      "collectCount": 12,
      "createdAt": "2026-06-10T12:00:00",
      "userId": 1,
      "username": "admin",
      "avatarUrl": "https://oss.example.com/avatars/1.jpg"
    }
  ],
  "total": 100,
  "page": 1,
  "pageSize": 20
}

// 后端 SQL 逻辑:
// SELECT p.id, p.title, p.cover_url, p.type,
//        p.like_count, p.comment_count, p.collect_count,
//        p.created_at, p.user_id,
//        u.username, u.avatar_url
// FROM posts p
// JOIN users u ON u.id = p.user_id
// WHERE p.status = 1 AND p.published_at IS NOT NULL
//   [AND p.id IN (SELECT post_id FROM post_tags WHERE tag_id = :tagId)]
//   [AND MATCH(p.title, p.content) AGAINST(:q)]
// ORDER BY CASE WHEN :sort='hot' THEN p.like_count END DESC, p.created_at DESC
// LIMIT :pageSize OFFSET (:page-1)*:pageSize
```

## 2.2 帖子详情
- **功能**: 查看单篇帖子的完整信息（含图片、标签、是否点赞）
- **路径**: `GET /posts/{id}`
- **需要 Token**

```json
// Response data
{
  "id": 1,
  "userId": 1,
  "title": "今天的穿搭分享",
  "content": "正文内容……",
  "coverUrl": "https://oss.example.com/cover/1.jpg",
  "type": 1,                        // 1=图文 2=视频
  "videoUrl": "https://oss.example.com/video/1.mp4",
  "location": "北京",
  "status": 1,                      // 0草稿 1发布
  "viewCount": 500,
  "likeCount": 128,
  "commentCount": 35,
  "collectCount": 12,
  "shareCount": 5,
  "createdAt": "2026-06-10T12:00:00",
  "publishedAt": "2026-06-10T12:00:00",
  "username": "admin",
  "avatarUrl": "https://oss.example.com/avatars/1.jpg",
  "isLiked": true,                  // 当前登录用户是否已点赞, 后端从 likes 表查询
  "images": [                       // 从 post_images 表查询
    { "id": 1, "url": "https://...", "sortOrder": 0, "width": 1080, "height": 720 },
    { "id": 2, "url": "https://...", "sortOrder": 1, "width": 1080, "height": 720 }
  ],
  "tags": [                         // 从 post_tags + tags 表查询
    { "id": 1, "name": "穿搭" },
    { "id": 3, "name": "旅行" }
  ]
}
```

## 2.3 创建帖子
- **功能**: 发布图文或视频笔记
- **路径**: `POST /posts`
- **需要 Token**

```json
// Request
{
  "title": "我的第一篇笔记",          // 必填
  "content": "正文内容……",           // 必填
  "type": 1,                         // 必填: 1=图文 2=视频
  "coverUrl": "https://cover.jpg",   // 封面URL
  "imageUrls": [                     // type=1 时传, 图片URL列表
    "https://img1.jpg",
    "https://img2.jpg"
  ],
  "videoUrl": "https://video.mp4",   // type=2 时传
  "tagIds": [1, 2, 3],              // 标签ID列表, 可选
  "location": "北京"                  // 发布地点, 可选
}

// Response data
{
  "id": 10,                          // 新帖ID
  "publishedAt": "2026-06-10T12:00:00"
}

// 后端需同时写入:
// 1. posts 表 (INSERT)
// 2. post_images 表 (INSERT 多条)
// 3. post_tags 表 (INSERT 多条)
// 4. 更新 tags 表的 post_count
```

## 2.4 删除帖子
- **功能**: 删除自己的帖子（逻辑删除或物理删除）
- **路径**: `DELETE /posts/{id}`
- **需要 Token**（仅作者或管理员）
- **Response data**: `null`

## 2.5 点赞帖子
- **功能**: 给帖子点赞，重复请求幂等（已点赞不重复插入）
- **路径**: `POST /posts/{id}/like`
- **需要 Token**
- **Response data**: `null`

```sql
-- 后端逻辑:
-- 1. 检查 likes 表: user_id=currentUser, target_type=1, target_id=postId
-- 2. 不存在则 INSERT INTO likes (user_id, target_type, target_id)
-- 3. UPDATE posts SET like_count = like_count + 1 WHERE id = postId
```

## 2.6 取消点赞帖子
- **功能**: 取消对帖子的点赞
- **路径**: `DELETE /posts/{id}/like`
- **需要 Token**
- **Response data**: `null`

```sql
-- 后端逻辑:
-- 1. DELETE FROM likes WHERE user_id=currentUser, target_type=1, target_id=postId
-- 2. UPDATE posts SET like_count = GREATEST(like_count - 1, 0) WHERE id = postId
```

## 2.7 收藏帖子
- **功能**: 将帖子收藏到指定收藏夹
- **路径**: `POST /posts/{id}/collect`
- **需要 Token**

```json
// Request
{
  "collectionId": 1
}

// Response data: null
```

---

# 三、评论模块

## 3.1 获取评论列表
- **功能**: 获取帖子的一级评论列表（分页，含回复数）
- **路径**: `GET /posts/{postId}/comments`
- **需要 Token**

```json
// Query Params: page=1, pageSize=20

// Response data
{
  "list": [
    {
      "id": 1,
      "postId": 1,
      "userId": 2,
      "content": "好棒的笔记！",
      "likeCount": 3,
      "replyCount": 2,             // 二级回复数 (子评论数)
      "createdAt": "2026-06-10T14:00:00",
      "username": "user2",
      "avatarUrl": "https://..."
    }
  ],
  "total": 10,
  "page": 1,
  "pageSize": 20
}
```

## 3.2 发布评论
- **功能**: 发表评论或回复别人的评论
- **路径**: `POST /comments`
- **需要 Token**

```json
// Request
{
  "postId": 1,           // 必填, 帖子ID
  "content": "写得好！",  // 必填, 评论内容
  "parentId": null,      // 选填, 回复某条评论时传父评论ID
  "replyToUserId": null  // 选填, 二级回复时传被回复人ID
}

// Response data
{
  "id": 20,
  "createdAt": "2026-06-10T14:30:00"
}

// 后端逻辑:
// 1. INSERT INTO comments
// 2. UPDATE posts SET comment_count = comment_count + 1
// 3. 创建通知（被评论人）
```

## 3.3 删除评论
- **功能**: 删除自己的评论
- **路径**: `DELETE /comments/{id}`
- **需要 Token**（仅作者或管理员）
- **Response data**: `null`

## 3.4 点赞评论
- **功能**: 给评论点赞
- **路径**: `POST /comments/{id}/like`
- **需要 Token**
- **Response data**: `null`

## 3.5 取消点赞评论
- **功能**: 取消对评论的点赞
- **路径**: `DELETE /comments/{id}/like`
- **需要 Token**
- **Response data**: `null`

---

# 四、标签模块

## 4.1 获取标签列表
- **功能**: 获取所有可用标签
- **路径**: `GET /tags`
- **无需 Token**

```json
// Response data
{
  "list": [
    { "id": 1, "name": "穿搭", "postCount": 100, "description": "时尚穿搭分享" },
    { "id": 2, "name": "美食", "postCount": 80, "description": "美食探店与食谱" },
    { "id": 3, "name": "旅行", "postCount": 60, "description": "旅行攻略与游记" },
    { "id": 4, "name": "美妆", "postCount": 45, "description": "美妆护肤心得" },
    { "id": 5, "name": "家居", "postCount": 30, "description": "家居装修灵感" },
    { "id": 6, "name": "数码", "postCount": 55, "description": "数码产品评测" },
    { "id": 7, "name": "健身", "postCount": 40, "description": "健身运动打卡" },
    { "id": 8, "name": "读书", "postCount": 25, "description": "读书笔记与推荐" },
    { "id": 9, "name": "摄影", "postCount": 35, "description": "摄影技巧与作品" },
    { "id": 10, "name": "手工", "postCount": 20, "description": "手工DIY教程" }
  ]
}
```

## 4.2 按标签浏览帖子
- **功能**: 查看某个标签下的帖子列表
- **路径**: `GET /tags/{tagId}/posts`
- **需要 Token**

```json
// Query Params: page=1, pageSize=20
// Response data: 同 2.1 瀑布流列表
```

---

# 五、收藏模块

## 5.1 获取收藏夹列表
- **功能**: 查看当前用户的收藏夹
- **路径**: `GET /collections`
- **需要 Token**

```json
// Response data
{
  "list": [
    {
      "id": 1,
      "userId": 1,
      "name": "想去的旅行",
      "description": "旅行灵感收藏",
      "coverUrl": "https://...",
      "isPublic": 1,         // 0私密 1公开
      "postCount": 5,
      "createdAt": "2026-06-01T12:00:00"
    }
  ]
}
```

## 5.2 获取收藏夹中的帖子
- **功能**: 查看某个收藏夹里的帖子
- **路径**: `GET /collections/{id}/posts`
- **需要 Token**

```json
// Query Params: page=1, pageSize=20
// Response data: 同 2.1 瀑布流列表
```

## 5.3 创建收藏夹
- **功能**: 新建一个收藏夹
- **路径**: `POST /collections`
- **需要 Token**

```json
// Request
{
  "name": "我的收藏",      // 必填
  "description": "描述",   // 选填
  "isPublic": 1           // 选填, 默认1公开
}

// Response data
{
  "id": 2,
  "createdAt": "2026-06-10T15:00:00"
}
```

---

# 六、通知模块

## 6.1 获取通知列表
- **功能**: 查看当前用户的通知，按时间倒序
- **路径**: `GET /notifications`
- **需要 Token**

```json
// Query Params: page=1, pageSize=20

// Response data
{
  "list": [
    {
      "id": 1,
      "type": 1,              // 1=点赞帖子 2=点赞评论 3=评论 4=回复 5=关注 6=收藏 7=系统
      "content": "赞了你的笔记「穿搭分享」",
      "isRead": 0,            // 0未读 1已读
      "createdAt": "2026-06-10T16:00:00",
      "senderId": 2,
      "senderName": "user2",
      "senderAvatar": "https://..."
    }
  ],
  "total": 5
}

-- 后端 SQL:
-- SELECT n.id, n.type, n.content, n.created_at, n.is_read,
--        s.id AS sender_id, s.username AS sender_name, s.avatar_url AS sender_avatar
-- FROM notifications n
-- LEFT JOIN users s ON s.id = n.sender_id
-- WHERE n.user_id = currentUserId
-- ORDER BY n.created_at DESC
-- LIMIT :pageSize OFFSET (:page-1)*:pageSize
```

## 6.2 获取未读通知数
- **功能**: 获取当前用户未读通知数量（侧边栏红点用）
- **路径**: `GET /notifications/unread-count`
- **需要 Token**

```json
// Response data
{ "count": 3 }
```

## 6.3 标记单条已读
- **功能**: 将指定通知标记为已读
- **路径**: `PUT /notifications/{id}/read`
- **需要 Token**
- **Response data**: `null`

## 6.4 全部标记已读
- **功能**: 将当前用户所有未读通知标记为已读
- **路径**: `PUT /notifications/read-all`
- **需要 Token**
- **Response data**: `null`

## 6.5 创建通知
- **功能**: 由其他业务操作（点赞、评论、关注、收藏等）触发，向指定用户发送通知。**发送者信息从 Token 中自动获取**。
- **路径**: `POST /notifications`
- **需要 Token**

```json
// Request
{
  "type": 1,              // 必填, 1=赞笔记 2=赞评论 3=评论 4=回复 5=关注 6=收藏
  "recipientId": 2,       // 必填, 接收通知的用户ID
  "targetId": 1,          // 必填, 被操作的目标ID（帖子/评论/用户）
  "targetType": 1         // 必填, 1=帖子 2=评论 0=用户（关注时）
}

// Response data: null
```

- **后端逻辑**:
  1. 从请求头 `token` 解析当前用户作为 `sender_id`
  2. 若 `sender_id == recipientId`（自己触发），直接返回成功，**不写入通知**
  3. 否则 `INSERT INTO notifications (user_id, type, sender_id, target_id, target_type)` 写入通知表

- **前端调用示例**:
```js
// src/api/index.js
createNotification(data) {
  return api.post('/notifications', data)
}

// 使用示例
import { createNotification } from '@/api'
createNotification({ type: 1, recipientId: post.userId, targetId: post.id, targetType: 1 })
```

---

# 七、搜索模块

## 7.1 搜索帖子
- **功能**: 全文搜索帖子标题和正文
- **路径**: `GET /search`
- **需要 Token**

```json
// Query Params
// q        | string | 必填, 搜索关键词
// page     | int    | 默认 1
// pageSize | int    | 默认 20

// 后端 SQL:
// SELECT p.id, p.title, p.content, p.like_count,
//        MATCH(p.title, p.content) AGAINST(:q) AS relevance
// FROM posts p
// WHERE MATCH(p.title, p.content) AGAINST(:q)
//   AND p.status = 1
// ORDER BY relevance DESC

// Response data: 同 2.1 瀑布流列表
```

## 7.2 获取搜索历史
- **功能**: 查看当前用户的搜索历史
- **路径**: `GET /search/history`
- **需要 Token**

```json
// Response data
{
  "list": [
    { "keyword": "穿搭", "createdAt": "2026-06-10T17:00:00" },
    { "keyword": "美食", "createdAt": "2026-06-10T16:00:00" }
  ]
}
```

## 7.3 清空搜索历史
- **功能**: 清除当前用户所有搜索历史
- **路径**: `DELETE /search/history`
- **需要 Token**
- **Response data**: `null`

---

# 八、文件上传模块

## 8.1 上传图片
- **功能**: 上传单张图片（支持 jpg/png/webp/gif）
- **路径**: `POST /upload/image`
- **需要 Token**
- **Content-Type**: `multipart/form-data`

```
FormData:
  file: File  ← 字段名 "file"

// Response data
{
  "url": "https://oss.example.com/images/abc123.jpg",
  "width": 1080,
  "height": 720
}
```

## 8.2 上传视频（含封面）
- **功能**: 上传视频文件，可选同时上传封面图片
- **路径**: `POST /upload/video`
- **需要 Token**
- **Content-Type**: `multipart/form-data`

```
FormData:
  file:  File  ← 视频文件，字段名 "file" (支持 mp4/mov)
  cover: File  ← 封面图片，字段名 "cover" (可选, 支持 jpg/png)

// Response data
{
  "videoUrl": "https://oss.example.com/videos/abc123.mp4",
  "coverUrl": "https://oss.example.com/covers/abc123.jpg"
}
```

---

# 九、数据库字段 → JSON 驼峰映射表

## posts（帖子表）
| 数据库字段 | JSON 字段 | Java 类型 | 说明 |
|---|---|---|---|
| `id` | `id` | Long | 主键 |
| `user_id` | `userId` | Long | 作者ID |
| `title` | `title` | String | 标题 |
| `content` | `content` | String | 正文 |
| `cover_url` | `coverUrl` | String | 封面图URL |
| `type` | `type` | Integer | 1=图文 2=视频 |
| `video_url` | `videoUrl` | String | 视频URL |
| `location` | `location` | String | 发布地点 |
| `status` | `status` | Integer | 0草稿 1发布 2审核 3违规 |
| `is_top` | `isTop` | Integer | 是否置顶 |
| `view_count` | `viewCount` | Long | 浏览数 |
| `like_count` | `likeCount` | Long | 点赞数 |
| `comment_count` | `commentCount` | Integer | 评论数 |
| `collect_count` | `collectCount` | Integer | 收藏数 |
| `share_count` | `shareCount` | Integer | 分享数 |
| `created_at` | `createdAt` | LocalDateTime | 创建时间 |
| `published_at` | `publishedAt` | LocalDateTime | 发布时间 |

## users（用户表）
| 数据库字段 | JSON 字段 | Java 类型 | 说明 |
|---|---|---|---|
| `id` | `id` | Long | 主键 |
| `username` | `username` | String | 用户名 |
| `phone` | `phone` | String | 手机号 |
| `email` | `email` | String | 邮箱 |
| `avatar_url` | `avatarUrl` | String | 头像URL |
| `bio` | `bio` | String | 个人简介 |
| `gender` | `gender` | Integer | 0未知 1男 2女 |
| `location` | `location` | String | 所在城市 |
| `status` | `status` | Integer | 0禁用 1正常 |
| `role` | `role` | Integer | 0普通 1管理员 |
| `created_at` | `createdAt` | LocalDateTime | 注册时间 |

## comments（评论表）
| 数据库字段 | JSON 字段 | Java 类型 | 说明 |
|---|---|---|---|
| `id` | `id` | Long | 主键 |
| `post_id` | `postId` | Long | 帖子ID |
| `user_id` | `userId` | Long | 评论人ID |
| `parent_id` | `parentId` | Long | 父评论ID(二级回复) |
| `reply_to_user_id` | `replyToUserId` | Long | 被回复人ID |
| `content` | `content` | String | 评论内容 |
| `like_count` | `likeCount` | Long | 点赞数 |
| `created_at` | `createdAt` | LocalDateTime | 评论时间 |

## notifications（通知表）
| 数据库字段 | JSON 字段 | 说明 |
|---|---|---|
| `id` | `id` | 主键 |
| `type` | `type` | 1赞帖 2赞评 3评论 4回复 5关注 6收藏 7系统 |
| `is_read` | `isRead` | 0未读 1已读 |
| `sender_id` | `senderId` | 触发者ID |
| `sender_name` | `senderName` | 触发者用户名（JOIN users 表） |
| `sender_avatar` | `senderAvatar` | 触发者头像 |
| `created_at` | `createdAt` | 通知时间 |

## post_images（帖子图片表）
| 数据库字段 | JSON 字段 | 说明 |
|---|---|---|
| `id` | `id` | 主键 |
| `post_id` | `postId` | 帖子ID |
| `url` | `url` | 图片URL |
| `sort_order` | `sortOrder` | 排序序号 |
| `width` | `width` | 图片宽度 |
| `height` | `height` | 图片高度 |

## tags（标签表）
| 数据库字段 | JSON 字段 | 说明 |
|---|---|---|
| `id` | `id` | 主键 |
| `name` | `name` | 标签名 |
| `post_count` | `postCount` | 关联帖子数 |
| `description` | `description` | 描述 |

# 十、错误码说明

| HTTP 状态 | 含义 | 前端处理 |
|---|---|---|
| 401 | Token 无效或过期 | 清除 token 和 user，跳转 `/login` |
| 404 | 资源不存在 | 显示对应提示（"帖子不存在"、"用户不存在"） |
| 415 | Content-Type 不支持 | 检查请求头（上传需 multipart，JSON 需 application/json） |
| 500 | 服务端异常 | 显示"服务器繁忙" |

后端返回 `{ code: 0, msg: "错误描述" }` 时，前端拦截器自动 `Promise.reject(new Error(msg))`，所有 API 的 `catch` 中可直接用 `e.message` 获取错误提示。