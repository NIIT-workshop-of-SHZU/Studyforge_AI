对话/文本模型
deepseek-ai/DeepSeek-V4-Flash
https://api.siliconflow.cn/v1
sk-sumhznadchbatbcaklzlttfzwocmqixrdaiicohoimpiuvpd

语音模型
FunAudioLLM/CosyVoice2-0.5B
https://api.siliconflow.cn/v1
sk-sumhznadchbatbcaklzlttfzwocmqixrdaiicohoimpiuvpd
示例：
curl --location 'https://api.siliconflow.cn/v1/audio/speech' \
--header 'Authorization: Bearer sk-xx' \
--header 'Content-Type: application/json' \
--data '{
  "model": "fnlp/MOSS-TTSD-v0.5",
  "input": "你站在桥上看风景，看风景的人在楼上看你。明月装饰了你的窗子，你装饰了别人的梦",
  "voice": "fnlp/MOSS-TTSD-v0.5:alex",
  "response_format": "mp3",
  "stream": true
}'

生图配置
gpt-image-2
https://api.hiyo.top/v1
sk-j1gAfU3lBu10wJrOoe0tdcleAd0KyWFa1FSSozbMqvHo31Hh
这是生图模型的配置，另外注意图像比例，而且当前模型也有图像编辑模式