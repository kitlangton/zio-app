import {resolve} from 'path'
import {minifyHtml, injectHtml} from 'vite-plugin-html'

const scalaVersion = '2.13'
// const scalaVersion = '3.0.0-RC3'

// https://vitejs.dev/config/
export default ({mode}) => {
    const mainJS = `./target/scala-${scalaVersion}/clifrontend-${mode === 'production' ? 'opt' : 'fastopt'}/main.js`
    const script = `<script type="module" src="${mainJS}"></script>`

    return {
        build: {
            outDir: '../cli/src/main/resources/dist',
            emptyOutDir: true
        },
        server: {
            port: 4000,
            proxy: {
                '/api': {
                    target: 'http://localhost:9630',
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
                'stylesheets': resolve(__dirname, './src/main/static/stylesheets'),
            }
        }
    }
}