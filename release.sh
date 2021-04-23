file=target/native-image/zio-app
tar -czf $file.gz $file
shasum -a 256 $file.gz
open target/native-image