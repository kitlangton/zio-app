sbt cliFrontend/fullLinkJS
cd cli-frontend
yarn exec -- vite build
cd ..

sbt cli/nativeImage
cd cli/target/native-image
mv zio-app-cli zio-app
tar -czf zio-app.gz zio-app
echo $(shasum -a 256 zio-app.gz)
open .
