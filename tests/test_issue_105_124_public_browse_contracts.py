import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class PublicBrowseContractsTest(unittest.TestCase):

    def test_issue_124_public_community_filters_visibility_and_has_frontend_list_detail(self):
        controller = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/controller/app/AigcCommunityAppController.java"
        )
        mapper = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/dal/mysql/AigcCommunityPostMapper.java"
        )
        list_page = read("yudao-ui/draw2video-client/src/app/(app)/community/page.tsx")
        detail_page = read("yudao-ui/draw2video-client/src/app/(app)/community/[id]/page.tsx")
        card = read("yudao-ui/draw2video-client/src/features/community/CommunityPostCard.tsx")

        self.assertIn("@GetMapping(\"/post/page\")", controller)
        self.assertIn("@GetMapping(\"/post/get\")", controller)
        self.assertIn("communityService.getPublicPostPage(reqVO, getLoginUserId())", controller)
        self.assertIn("communityService.getPublicPost(id, getLoginUserId())", controller)
        self.assertIn("publicWrapper()", mapper)
        self.assertIn("AigcCommunityPostStatusEnum.PUBLISHED.getCode()", mapper)
        self.assertIn("AigcCommunityAuditStatusEnum.PASS.getCode()", mapper)
        self.assertIn("orderFeed(wrapper, reqVO.getSort())", mapper)
        self.assertIn("getCommunityPosts({ pageNo: 1, pageSize: 30, sort, keyword })", list_page)
        self.assertIn("No public works yet.", list_page)
        self.assertIn("CommunityPostCard", list_page)
        self.assertIn("getCommunityPost(postKey)", detail_page)
        self.assertIn("post.fileUrl || post.coverUrl", detail_page)
        self.assertIn("post.likeCount", detail_page)
        self.assertIn("post.commentCount", detail_page)
        self.assertIn("post.shareCount", detail_page)
        self.assertIn("authorNickname", card)

    def test_issue_105_frontend_guide_browses_published_content_by_business_module(self):
        controller = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/controller/guide/AigcGuideContentController.java"
        )
        mapper = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/dal/mysql/AigcGuideContentMapper.java"
        )
        api = read("yudao-ui/draw2video-client/src/features/guide/guide-api.ts")
        list_page = read("yudao-ui/draw2video-client/src/app/(app)/guide/page.tsx")
        detail_page = read("yudao-ui/draw2video-client/src/app/(app)/guide/[slug]/page.tsx")

        self.assertIn("@GetMapping(\"/aigc/guide/content/list\")", controller)
        self.assertIn("@GetMapping(\"/aigc/guide/content/public-get\")", controller)
        self.assertIn("guideContentService.getPublishedContentList()", controller)
        self.assertIn("guideContentService.getPublishedContent(slug)", controller)
        self.assertIn("AigcGuidePublishStatusEnum.PUBLISHED.getCode()", mapper)
        self.assertIn("selectPublishedBySlug", mapper)
        self.assertIn("getPublishedGuides", api)
        self.assertIn("getPublishedGuide", api)
        self.assertIn("/aigc/guide/content/list", api)
        self.assertIn("/aigc/guide/content/public-get", api)
        self.assertIn("groupByCategory", list_page)
        self.assertIn("activeCategory", list_page)
        self.assertIn("当前模块暂无已发布指南", list_page)
        self.assertIn("搜索指南标题、模块或正文", list_page)
        self.assertIn("getPublishedGuide(slug)", detail_page)
        self.assertIn("指南不存在或尚未发布", detail_page)
        self.assertIn("renderContent(guide?.content)", detail_page)


if __name__ == "__main__":
    unittest.main()

