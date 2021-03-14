# 高鐵訂票小幫手

- [高鐵訂票小幫手](#高鐵訂票小幫手)
  - [Installation](#installation)
  - [Inference](#inference)
  - [注意事項!!!](#注意事項)
  - [Android](#android)

The original code comes from [BreezeWhite/THSR-Ticket](https://github.com/BreezeWhite/THSR-Ticket). Thanks for the good work!


- 2021 / 02 / 19 - Modify booking process, one can book a ticket with simply modifying `db/myBooking_sample.json`, and also booking with `car_id` is supported now


![](https://github.com/BreezeWhite/THSR-Ticket/workflows/build/badge.svg)

**!!--純研究用途，請勿用於不當用途--!!**

此程式提供另一種輕便的方式訂購高鐵車票，操作介面為命令列介面。相較於使用網頁訂購，本程式因為省卻了渲染網頁介面的時間，只保留最核心的訂購功能，因此能省下大量等待的時間。

## Installation

### Conda
```
conda env create -f environment.yml
conda activate THSR
```

### pip 
```
pip install -r requirements.txt
```

## Inference

1. set up [db/myBooking_sample.json](db/myBooking_sample.json) if you are not going to enter things step by step
   ```
   // myBooking_sample.json
   {
    "_default": {
        "1": {
            "adult_num": "1F",              // How many "adults" you have
            "car_id": 633,                  // specify car_id if needed
            "dest_station": 5,              // set destination station
            "outbound_date": "2021/02/19",  // set date 
            "outbound_time": "1201A",       // set time
            "personal_id": "AXXXXXXXXX",    // set personal_id
            "phone": "09XXXXXXXX",          // set phone number
            "search_by": 1,                 // 0: search by time, 1: seach by car_id
            "start_station": 1              // set start station
        }
    }
    }   
   ```

2. run
    ```
    python thsr_ticket/main.py
    ```

## 注意事項!!!

本程式依舊有許多尚未完成的部分，僅具備基本訂購的功能，若是僅需要訂購成人票、且無特殊需求者，此程式對您而言是加速訂購流程的方便小工具。不符合以上描述者，目前仍建議使用官方網頁進行訂購。

#### 提供功能

- [x] 選擇啟程、到達站
- [x] 選擇出發日期、時間
- [x] 選擇班次
- [x] 選擇**"成人"**票數
- [x] 輸入驗證碼
- [x] 輸入身分證字號
- [x] 輸入手機號碼
- [x] 保留此次輸入紀錄，下次可快速選擇此次紀錄
- [x] 訂位方式(依時間搜尋車次/直接輸入車次號碼)

#### 未提供功能

以下功能為未提供輸入的選項，但程式具備相關功能，可依照自身需求、對程式進行修改

- [ ] 選擇車廂種類(標準/商務)
- [ ] 座位喜好(靠窗/走道)
- [ ] 輸入孩童/愛心/敬老/學生優惠票數
- [ ] 僅顯示早鳥優惠票

#### 未完成功能

- [ ] 重新產生認證碼
- [ ] 語音播放認證碼
- [ ] 重新查詢車次
- [ ] 輸入護照號碼
- [ ] 輸入市話
- [ ] 輸入電子郵件
- [ ] 會員購票

## Android

Android app for fast ticket booking, solving captcha with tflite model.

<img src=imgs/android.png>