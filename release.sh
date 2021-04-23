sbt nativeImage
cd ./target/native-image
echo `pwd`
tar -czf zio-app.gz zio-app
shasum -a 256 zio-app.gz
open .
