sbt cli/nativeImage
cd cli/target/native-image
echo $(pwd)
mv zio-app-cli zio-app
echo $(ls)
tar -czf zio-app.gz zio-app
echo $(ls)
