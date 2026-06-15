import type { Config } from '@docusaurus/types'
import type * as Preset from '@docusaurus/preset-classic'
import { themes } from 'prism-react-renderer'

const config: Config = {
  title: 'Draw2Video 使用指南',
  tagline: 'AIGC 平台使用指南与集成说明',

  url: 'https://docs.draw2video.local',
  baseUrl: '/guide/',

  organizationName: 'draw2video',
  projectName: 'draw2video-guide',

  onBrokenLinks: 'throw',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn'
    }
  },
  trailingSlash: false,

  i18n: {
    defaultLocale: 'zh-CN',
    locales: ['zh-CN']
  },

  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: '/',
          sidebarPath: './sidebars.ts'
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css'
        }
      } satisfies Preset.Options
    ]
  ],

  themeConfig: {
    navbar: {
      title: 'Draw2Video 使用指南',
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'guideSidebar',
          position: 'left',
          label: '指南'
        }
      ]
    },
    footer: {
      style: 'dark',
      copyright: `Copyright © ${new Date().getFullYear()} Draw2Video`
    },
    prism: {
      theme: themes.github,
      darkTheme: themes.dracula
    }
  } satisfies Preset.ThemeConfig
}

export default config
