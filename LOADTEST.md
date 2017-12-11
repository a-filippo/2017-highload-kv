## Поехали
*В основом я делаю замеры с помощью метода `put`, поскольку внутри себя он отправляет запросы в бд и на получение, и на запись. Все узкие места (скорее всего) можно будет найти этим методом.* 

```shell
afilippomr:scripts afilippo$ wrk --latency -c4 -t2 -d5m -s put.lua http://localhost:8080
Running 5m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   116.07ms  260.33ms   1.98s    87.69%
    Req/Sec   238.63    152.06   570.00     62.61%
  Latency Distribution
     50%    8.47ms
     75%   20.16ms
     90%  461.61ms
     99%    1.27s
  100005 requests in 5.00m, 8.96MB read
  Socket errors: connect 0, read 0, write 0, timeout 25
  Non-2xx or 3xx responses: 1
Requests/sec:    333.26
Transfer/sec:     30.59KB
```
Все очень плохо: 25 таймаутов, низкая скорость записи и всего 100К запросов за 5 минут.

*results/put-at-start.svg*

Нужно что-то менять

Первым шагом добавим пул соединений к БД стандартным методом.
Стало еще хуже.

*results/put-after-adding-pool.svg*

Откатим обратно. Плохое решение.
Поймем, что наше имеющееся решение куда лучше, поскольку мы кэшируем `PreparedStatement` каждого запроса в базу. Соответственно, `Connection` кэшируется вместе с ним. Оставим пока так.

Нужно что-то сделать. Исправим http-клиент, который долбится в реплики. Создадим клиента только один раз, а затем будем использовать его при каждом обращении к репликам.

*results/put-norm.svg*

Уже лучше. Думаем дальше.
Ах да, сервер же синхронный, добавим асинхронности. 8 потоков думаю пока достаточно.

*Здесь уже не осталось вывода `wrk` в `bash`, неатомарность коммитов взяла свое, состояние не повторить, косяк понял. Восстанавливаем по памяти...*

Хм, начинает падать БД. Запросы идут конкурентно и пытаются изменить одну и ту же часть базы одновременно. Вот она асинхронность.
Перепишем наши кэшированные `PreparedStatement` в пул из `PreparedStatement`.

Так, а здесь можно посмотреть результат `wrk`.

```shell
afilippomr:scripts afilippo$ wrk --latency -c4 -t2 -d5m -s put.lua http://localhost:8080
Running 5m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.22ms    7.05ms 264.71ms   95.93%
    Req/Sec     1.51k   258.63     1.99k    71.77%
  Latency Distribution
     50%    1.13ms
     75%    1.28ms
     90%    1.97ms
     99%   23.06ms
  900067 requests in 5.00m, 80.69MB read
Requests/sec:   2999.46
Transfer/sec:    275.34KB
```

Хм, надо погуглить перфоманс встраиваемых СУБД... Может быть, Derby не лучшее решение?
Попробуем H2

## H2 fixed pool - 100 потоков на запросы к репликам
```shell
afilippomr:scripts afilippo$ wrk --latency -c4 -t2 -d5m -s put.lua http://localhost:8080
Running 5m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     4.93ms   28.52ms 474.39ms   97.65%
    Req/Sec     1.96k   345.12     2.67k    78.91%
  Latency Distribution
     50%    0.93ms
     75%    1.05ms
     90%    1.25ms
     99%  149.61ms
  1154935 requests in 5.00m, 103.53MB read
Requests/sec:   3849.34
Transfer/sec:    353.36KB
```

## H2 fixed pool - 24 потока на запросы к репликам
```shell
afilippomr:scripts afilippo$ wrk --latency -c4 -t2 -d5m -s put.lua http://localhost:8080
Running 5m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     5.51ms   31.16ms 462.66ms   97.39%
    Req/Sec     2.11k   388.79     2.80k    80.26%
  Latency Distribution
     50%    0.85ms
     75%    0.96ms
     90%    1.18ms
     99%  181.40ms
  1243504 requests in 5.00m, 111.47MB read
Requests/sec:   4144.54
Transfer/sec:    380.46KB
```

Результаты довольно неоднозначные. 
Derby дает преимущество в более низкой максимальной задержке, но с H2 происходит на треть запросов больше.

*results/h2.svg*

Случайная высокая задержка обусловлена внутренней работой СУБД. Как решение - можно реализовать перед моделью БД слой, который будет кэшировать все записи в базу в памяти, а в отдельном потоке писать их пачками уже непосредственно в базу. Но этим решением можно будет повысить производительность только на запись, что делать с чтением - мне в голову не пришло.

Возможно, проблема с тем, что кластеру приходится взаимодействовать одновременно с тремя базами при высокой нагрузке. И все это дело упирается во взаимодействие с диском.

Попробуем сделать задержку между `http` запросами в `wrk`, тем самым снизить нагрузку на СУБД.

```shell
afilippomr:scripts afilippo$ wrk --latency -c1 -t1 -d10s -s put.lua http://localhost:8080
Running 10s test @ http://localhost:8080
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.19ms  109.24us   1.71ms   83.36%
    Req/Sec    54.55      5.02    60.00     48.48%
  Latency Distribution
     50%    1.20ms
     75%    1.25ms
     90%    1.29ms
     99%    1.38ms
  547 requests in 10.02s, 50.21KB read
Requests/sec:     54.60
Transfer/sec:      5.01KB

...

afilippomr:scripts afilippo$ wrk --latency -c1 -t1 -d10s -s put.lua http://localhost:8080
Running 10s test @ http://localhost:8080
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.18ms  753.77us  18.67ms   99.82%
    Req/Sec    55.87      4.91    60.00     64.65%
  Latency Distribution
     50%    1.18ms
     75%    1.22ms
     90%    1.27ms
     99%    1.39ms
  559 requests in 10.01s, 51.31KB read
Requests/sec:     55.86
Transfer/sec:      5.13KB
```
Видимо проблема все таки во внутренней работе СУБД.

Следующие результаты для H2 (почему то оставил ее, наверное погнался за цифрами (а может и не стоило этого делать))

```shell
afilippomr:scripts afilippo$ wrk --latency -c4 -t2 -d5m -s put-3-3.lua http://localhost:8080
Running 5m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     6.05ms   33.27ms 450.11ms   97.28%
    Req/Sec     2.03k   354.05     2.73k    79.40%
  Latency Distribution
     50%    0.90ms
     75%    1.01ms
     90%    1.20ms
     99%  205.44ms
  1192007 requests in 5.00m, 106.86MB read
Requests/sec:   3973.31
Transfer/sec:    364.74KB

...

afilippomr:scripts afilippo$ wrk --latency -c4 -t2 -d1m -s get-2-3.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     3.16ms   18.74ms 378.18ms   97.46%
    Req/Sec     2.33k   361.37     2.91k    80.54%
  Latency Distribution
     50%  776.00us
     75%    0.86ms
     90%    1.03ms
     99%   50.95ms
  276641 requests in 1.00m, 43.00MB read
Requests/sec:   4606.97
Transfer/sec:    733.34KB

...

afilippomr:scripts afilippo$ wrk --latency -c4 -t2 -d1m -s get-3-3.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  2 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     7.84ms   44.29ms 534.82ms   97.10%
    Req/Sec     2.40k   324.05     2.91k    90.43%
  Latency Distribution
     50%  778.00us
     75%  845.00us
     90%    0.96ms
     99%  274.49ms
  280511 requests in 1.00m, 43.61MB read
Requests/sec:   4668.81
Transfer/sec:    743.18KB
```

Вроде бы все