

-- 滑动窗口限流
local key = KEYS[1]                         -- 用于标识请求类型或用户的key
local maxCount = tonumber(ARGV[1])          -- 最大请求次数
local timeRange = tonumber(ARGV[2])         -- 滑动窗口的大小（秒）
local currentTimeMillis = tonumber(ARGV[3]) -- 当前请求的时间戳（毫秒）

local nowCount = redis.call('ZCARD', key)  --先检查当前数量，如果大于最大请求次数，则直接返回
if nowCount >= maxCount then
    return 0                                -- 返回0，表示请求被拒绝
end

                                            -- 否则，将当前时间戳添加到滑动窗口中
redis.call('ZADD', key, currentTimeMillis, currentTimeMillis)
redis.call('EXPIRE', key, timeRange)

local minScore = currentTimeMillis - timeRange * 1000
redis.call('ZREMRANGEBYSCORE', key, '-inf', minScore)

return 1;                                   -- 返回1，表示请求被接受



