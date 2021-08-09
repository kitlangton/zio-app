BUILD_ENV=prod sbt frontend/fullLinkJS
yarn exec vite -- build
cp dist/index.html dist/200.html
cp frontend/target/scala-2.13/frontend*jar/prod/shocon.conf dist/assets/shocon.conf
surge ./dist '$name$.surge.sh'
