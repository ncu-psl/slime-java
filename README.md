# SlimeJava

SlimeJava 是一個對於 Java 的源對源轉換器，透過標註 (Annotation) 的方式阻止表示暴露問題發生

## 環境

- [Kotlin](https://kotlinlang.org/) - 開發語言
- [Gradle](https://gradle.org/) - 建構 Kotlin 專案工具
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) - IDE

## 運作方式

SlimeJava 因為需要編譯，以下提供編譯方法，接著再進行執行準備及流程，將分為範例程式及效能評估測試

### 建置

#### 已編譯版本

已將編譯完成的 jar 檔案放置在 [Releases](https://github.com/ncu-psl/slime-java/releases) 上

#### 自行編譯

因為這是已經透過 gradle 建置好的專案，你在下載完後整個專案後可以：

- 直接透過 gradlew 編譯，直接在專案根目錄執行

```Batch
gradlew build
```

- 或使用 IntelliJ IDEA 打開整個專案檢視架構 (不建議使用 [VScode 有問題](https://github.com/ncu-psl/slime-java/issues/3))

### 執行準備及流程

- 需要先安裝好 [Java 17](https://adoptium.net/temurin/releases/)
- 放置與[測試資料](./app/src/testcase/)同資料夾即可

#### 執行範例程式測試

- SlimeJava 流程

    執行以下 Jar 指令並帶入 Testcase 檔名

    ```Batch
    java -jar SlimeJava.jar MementoTest.java
    ```

    過程將會記錄在生成的 logs 資料夾中，若有錯誤會同時打印出來

- 一般 Java 流程

    執行以下編譯指令並帶入 Testcase 檔名

    ```Batch
    javac MementoTest.java
    ```

結束後會分成數個 class 檔案，找到 MementoTest$Main 並執行以下指令

```Batch
java MementoTest$Main
```

裡面會有 Tweet object 分別為 `firstTweet`、`secondTweet` 直接打印出其推文評論之資料 `getCommentList()`，結果會顯示於 Console 中：

- 在使用 SlimeJava 下，二者的別名互相參考得以阻止而視為二種不同的物件

    ```Java
    [A] // firstTweet
    [A, B] // secondTweet
    ```

- 在一般 Java 的結果如下，二者結果會因為別名機制造成表示暴露問題

    ```Java
    [A, B] // firstTweet
    [A, B] // secondTweet
    ```

#### 執行效能評估測試

分成二個部分編譯，分別執行以下 Jar 指令並帶入 Testcase 檔名，並且以 Java 編譯器再次編譯

```Batch
java -jar SlimeJava.jar MementoEvalTest.java
javac MementoEvalTest.java
```

執行分成二個選擇，單次及批次

- 單次執行

    ```Batch
    java MementoEvalTest$Main
    ```

- 批次執行

    在 Testcase 中有提供 [python 檔案](./app/src/testcase/eval_tool.py)作為批次執行輔助工具，需求為 Python 3.10

    ```Batch
    python eval_tool.py
    ```

    預設為將 DataFrame 以圖表顯示，若使用 VSCode 的話可以透過安裝 [Jupyter 插件](https://marketplace.visualstudio.com/items?itemName=ms-toolsai.jupyter)來檢視，抑或是自行處理 DataFrame 呈現方式

接著將顯示出所花費之執行時間 (毫秒)

- 在使用 SlimeJava 下，大約落在 13 ~ 21 毫秒
- 在一般 Java 的結果，大約落在 9 ~ 17 毫秒

## 標註功能

這裡會介紹所有標註功能

### SlimeOwned

讓物件具有所有權，能夠使內部成員被類別所持有

- 宣告所有權—因 `Owner` class 中持有其內部成員 `mObj`，我們可以對 `mObj` 宣告 `SlimeOwned`，代表類別 `Owner` 持有 `mObj` 的所有權

    ```Java
    class Owner {
        @SlimeOwned Object mObj;
    }  
    ```  

- Setter —當我們需要對 `mObj` 賦值 `newObj` 時，表示成如下結果：

    ```Java
    public void setObj(Object newObj) {
        mObj = newObj;
    }       
    ```

    因為 `mObj` 已宣告了 `SlimeOwned`，代表為了消除外部參考可能造成的表示暴露問題，因此將會對任何 `mObj` 之賦值行為轉換成深度複製以持有所有權。因此，我們將透過 SlimeJava 轉換後的程式碼，表示成以下結果：

    ```Java
    public void setObj(Object newObj) {
        mObj = Object_Copy.deepCopy(newObj);
    }    
    ```

- Getter —當我們需要將 `mObj` 提供給外部使用時，那麼我們必須將該資料標記為 `SlimeOwned` 以便識別

    ```Java
    @SlimeOwned public Object getObj() {
        return mObj; // OK
    }    
    ```

    反之，若未標記而回傳 `SlimeOwned` 有關的內部成員，將發生轉換錯誤

    ```Java
    public Object getObj() {
        return mObj; // Error: Cannot return owned `mObj` to non-owned method `getObj`
    }    
    ```

### SlimeBorrow

借用已持有之物件

- 從 `SlimeOwned` 借用—跟 `Owner` 借用 `List<String>` 來操作其值

    ```Java
    Owner owner = new Owner(List.of("Hello", " ", "World"));
    @SlimeBorrow List<String> list = owner.getList(); // OK
    ```

  反之，無法賦值給 `SlimeOwned`

    ```Java
    Owner(@SlimeBorrow List<String> list) {
        mList = list; // Error: Cannot assign borrow `list` to owned `mList`
    }
    ```

- 無法提供非預期別名—若為非 `SlimeBorrow` 借用之別名將會發生賦值行為錯誤
  
    ```Java
    List<String> aliasList = list; // Error: Cannot assign to non-borrow `aliasList`
    ```

### SlimeCopy

複製已持有之物件

- 複製整個 `List` —當我們想要透過已經寫好的 Method `printListAllString` 來印出 `List` 中所有字串時，我們可能會使用到複製功能

    ```Java
    public void printListAllString(List<String> strList)
    ```

  此時我們可以對 `Owner::getList()` 使用 `SlimeCopy` 來做深度複製

    ```Java
    Owner owner = new Owner(List.of("Hello", " ", "World"));
    @SlimeCopy final List<String> copyList = owner.getList();
    printListAllString(copyList); // OK
    ```
