sbt cli/nativeImage
cd ./cli/target/native-image
mv zio-app-cli zio-app
tar -czf zio-app.gz zio-app
shasum -a 256 zio-app.gz
open .