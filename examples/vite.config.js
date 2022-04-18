import {resolve} from 'path'
import {minifyHtml, injectHtml} from 'vite-plugin-html'

const scalaVersion = '2.13'
// const scalaVersion = '3.0.0-RC3'


// https://vitejs.dev/config/
export default ({mode}) => {
    const mainJS = `./js/target/scala-${scalaVersion}/zio-app-examples-${mode === 'production' ? 'opt' : 'fastopt'}/main.js`
    const script = `<script type="module" src="${mainJS}"></script>`

    return {
        server: {
            proxy: {
                '/api': {
                    target: 'http://localhost:8088',
                    changeOrigin: true,
                    rewrite: (path) => path.replace(/^\/api/, '')
                },
            }
        },
//         publicDir: './src/main/static/public',
        plugins: [
            ...(process.env.NODE_ENV === 'production' ? [minifyHtml(),] : []),
            injectHtml({
                injectData: {
                    script
                }
            })
        ],
        resolve: {
            alias: {
                'stylesheets': resolve(__dirname, './frontend/src/main/static/stylesheets'),
            }
        }
    }
}