#!/bin/bash

# 清理IDEA中的Maven相关缓存，解决依赖问题

echo "🔧 清理IDEA中的Maven缓存..."

# 1. 清理Maven仓库中的缓存错误文件
echo "清理Maven仓库错误缓存..."
find /home/zhx/.m2/repository -name "*.lastUpdated" -type f -delete
find /home/zhx/.m2/repository -name "_remote.repositories" -type f -delete

# 2. 重新安装Huobi依赖
echo "重新安装Huobi依赖..."
cd /home/zhx/dev/java/api
mvn install:install-file -Dfile=1.0.1/huobi-linear-swap-api-1.0.1.jar -DgroupId=com.huobi.linear.swap.api -DartifactId=huobi-linear-swap-api -Dversion=1.0.1 -Dpackaging=jar -DgeneratePom=true

# 3. 验证依赖是否正确安装
echo "验证依赖..."
if [ -f "/home/zhx/.m2/repository/com/huobi/linear/swap/api/huobi-linear-swap-api/1.0.1/huobi-linear-swap-api-1.0.1.jar" ]; then
    echo "✅ Huobi依赖安装成功"
else
    echo "❌ Huobi依赖安装失败"
    exit 1
fi

# 4. 测试编译
echo "测试编译..."
mvn clean compile -Dmaven.repo.local=/home/zhx/.m2/repository

if [ $? -eq 0 ]; then
    echo "✅ 编译成功！"
    echo ""
    echo "📋 IDEA配置建议："
    echo "1. 打开IDEA Settings (Ctrl+Alt+S)"
    echo "2. 搜索 'Maven'"
    echo "3. 设置 Maven home directory 为: /home/zhx/.sdkman/candidates/maven/current"
    echo "4. 设置 User settings file 为: /home/zhx/.m2/settings.xml"
    echo "5. 设置 Local repository 为: /home/zhx/.m2/repository"
    echo "6. 点击 'Reload All Maven Projects' 按钮"
    echo ""
    echo "🎉 所有配置完成！项目应该可以在IDEA中正常运行了。"
else
    echo "❌ 编译失败，请检查错误信息"
    exit 1
fi