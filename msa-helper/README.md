miit_mdid_1.0.10.aar msa1.0.10，用于获取oaid， 仅用于releasedemo测试
miit_mdid_1.0.10.jar aar解压获取的jar， 用于compile
msa_helper.jar 适配1.0.10的OaidHelper1010 以及1.0.13的OaidHelper
msa_mdid_1.0.13.aar msa1.0.13, 用于获取oaid， 仅用于releasedemo测试
msa_mdid_1.0.13.jar aar解压获取的jar， 用于compile
oaid_sdk_1.0.22.aar msa1.0.22, 用于获取oaid， 仅用于releasedemo测试
oaid_sdk_1.0.22.jar aar解压获取的jar， 用于compile
classes目录下为编译后的OaidHelper类, 用于编译新的jar


## 编译成jar的方式
分别拿到各个类的class文件，再按包名的目录结构摆放。
jar cvf msa_helper.jar *

## 注意事项
1.0.26增加了证书校验，可以使用本模块assets目录下证书对应报名的应用进行测试