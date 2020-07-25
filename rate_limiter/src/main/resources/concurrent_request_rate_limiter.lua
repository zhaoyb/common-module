
-- 使用hash表保存数据
local ratelimit_info = redis.pcall('HMGET',KEYS[1],'last_time','current_token')
-- 最后更新时间
local last_time = ratelimit_info[1]
-- 当前剩余的token
local current_token = tonumber(ratelimit_info[2])
-- 参数 最大token数
local max_token = tonumber(ARGV[1])
-- 参数 token 生成速率
local token_rate = tonumber(ARGV[2])
-- 参数 当前时间
local current_time = tonumber(ARGV[3])
-- 表示 往桶中放一个token的间隔时间
local reverse_time = 1000/token_rate
-- 如果current_token为空，则默认当前桶是满的，最后更新时间为当前时间
if current_token == nil then
  current_token = max_token
  last_time = current_time
else
  -- 表示距离上一次操作过去了多长时间
  local past_time = current_time-last_time
  -- 表示 根据时间差，算出在过去的这段时间，应该放入多少个token, 因为没有办法通过一个线程定时往桶中放令牌，只能通过这种时间差的方式
  local reverse_token = math.floor(past_time/reverse_time)
  -- 之前的剩余token 加上 过去这段时间 应该放进去的
  current_token = current_token+reverse_token
  last_time = reverse_time*reverse_token+last_time
  -- 桶中令牌溢出，默认最大令牌数
  if current_token>max_token then
    current_token = max_token
  end
end
local result = 0
if(current_token>0) then
  result = 1
  current_token = current_token-1
end
redis.call('HMSET',KEYS[1],'last_time',last_time,'current_token',current_token)
-- 过期时间
redis.call('pexpire',KEYS[1],math.ceil(reverse_time*(max_token-current_token)+(current_time-last_time)))
return result