#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from typing import Any


DEFAULT_BASE_URL = "http://111.228.39.103:3000"
DEFAULT_OWNER = "root"
DEFAULT_REPO = "manman"
DEFAULT_TIMEOUT = 30


class GiteaApiError(RuntimeError):
    def __init__(self, status: int, message: str) -> None:
        super().__init__(f"Gitea API 请求失败({status}): {message}")
        self.status = status
        self.message = message


class GiteaTechManager:
    def __init__(
        self,
        base_url: str = DEFAULT_BASE_URL,
        owner: str = DEFAULT_OWNER,
        repo: str = DEFAULT_REPO,
        token: str | None = None,
        username: str | None = None,
        password: str | None = None,
        timeout: int = DEFAULT_TIMEOUT,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.owner = owner
        self.repo = repo
        self.token = token or os.getenv("GITEA_TOKEN")
        self.username = username or os.getenv("GITEA_USERNAME")
        self.password = password or os.getenv("GITEA_PASSWORD")
        self.timeout = timeout

    @property
    def repo_path(self) -> str:
        return f"/api/v1/repos/{quote_path(self.owner)}/{quote_path(self.repo)}"

    def request(
        self,
        method: str,
        path: str,
        query: dict[str, Any] | None = None,
        body: dict[str, Any] | None = None,
    ) -> Any:
        url = f"{self.base_url}{path}"
        if query:
            clean_query = {
                key: value
                for key, value in query.items()
                if value is not None and value != ""
            }
            if clean_query:
                url = f"{url}?{urllib.parse.urlencode(clean_query, doseq=True)}"

        data = None
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
        }
        if self.token:
            headers["Authorization"] = f"token {self.token}"
        elif self.username and self.password:
            credential = f"{self.username}:{self.password}".encode("utf-8")
            headers["Authorization"] = f"Basic {base64.b64encode(credential).decode('ascii')}"
        if body is not None:
            data = json.dumps(body, ensure_ascii=False).encode("utf-8")

        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                content = response.read().decode("utf-8")
                if not content:
                    return None
                return json.loads(content)
        except urllib.error.HTTPError as error:
            content = error.read().decode("utf-8", errors="replace")
            raise GiteaApiError(error.code, content or error.reason) from error
        except urllib.error.URLError as error:
            raise RuntimeError(f"无法连接 Gitea: {error.reason}") from error

    def list_issues(
        self,
        labels: list[str] | None = None,
        state: str = "open",
        page: int = 1,
        limit: int = 20,
    ) -> list[dict[str, Any]]:
        return self.request(
            "GET",
            f"{self.repo_path}/issues",
            {
                "state": state,
                "labels": ",".join(labels or []),
                "page": page,
                "limit": limit,
            },
        )

    def get_issue(self, index: int) -> dict[str, Any]:
        return self.request("GET", f"{self.repo_path}/issues/{index}")

    def create_issue(
        self,
        title: str,
        body: str | None = None,
        labels: list[str] | None = None,
        assignees: list[str] | None = None,
        milestone: int | None = None,
        due_date: str | None = None,
    ) -> dict[str, Any]:
        payload: dict[str, Any] = {"title": title}
        if body:
            payload["body"] = body
        if labels:
            payload["labels"] = labels
        if assignees:
            payload["assignees"] = assignees
        if milestone is not None:
            payload["milestone"] = milestone
        if due_date:
            payload["due_date"] = due_date
        return self.request("POST", f"{self.repo_path}/issues", body=payload)

    def list_labels(self) -> list[dict[str, Any]]:
        return self.request("GET", f"{self.repo_path}/labels")

    def set_issue_labels(self, index: int, labels: list[str]) -> list[dict[str, Any]]:
        return self.request(
            "PUT",
            f"{self.repo_path}/issues/{index}/labels",
            body={"labels": labels},
        )

    def add_issue_labels(self, index: int, labels: list[str]) -> list[dict[str, Any]]:
        return self.request(
            "POST",
            f"{self.repo_path}/issues/{index}/labels",
            body={"labels": labels},
        )

    def remove_issue_label(self, index: int, label: str) -> None:
        label_id = self.find_label_id(label)
        self.request("DELETE", f"{self.repo_path}/issues/{index}/labels/{label_id}")

    def comment_issue(self, index: int, body: str) -> dict[str, Any]:
        return self.request(
            "POST",
            f"{self.repo_path}/issues/{index}/comments",
            body={"body": body},
        )

    def find_label_id(self, label: str) -> int:
        labels = self.list_labels()
        for item in labels:
            if item.get("name") == label or str(item.get("id")) == label:
                return int(item["id"])
        names = ", ".join(str(item.get("name")) for item in labels)
        raise ValueError(f"未找到标签 {label!r}，当前标签：{names}")


def quote_path(value: str) -> str:
    return urllib.parse.quote(value, safe="")


def parse_labels(value: str | None) -> list[str]:
    if not value:
        return []
    return [item.strip() for item in value.split(",") if item.strip()]


def print_json(data: Any) -> None:
    print(json.dumps(data, ensure_ascii=False, indent=2))


def print_issue_table(issues: list[dict[str, Any]]) -> None:
    if not issues:
        print("未找到 issue")
        return
    for issue in issues:
        labels = ", ".join(label.get("name", "") for label in issue.get("labels", []))
        print(f"#{issue.get('number')} [{issue.get('state')}] {issue.get('title')}")
        print(f"  标签: {labels or '-'}")
        print(f"  地址: {issue.get('html_url')}")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Gitea manman issue 管理工具。默认仓库：http://111.228.39.103:3000/root/manman/issues",
    )
    parser.add_argument("--base-url", default=os.getenv("GITEA_BASE_URL", DEFAULT_BASE_URL))
    parser.add_argument("--owner", default=os.getenv("GITEA_OWNER", DEFAULT_OWNER))
    parser.add_argument("--repo", default=os.getenv("GITEA_REPO", DEFAULT_REPO))
    parser.add_argument("--token", default=os.getenv("GITEA_TOKEN"))
    parser.add_argument("--username", default=os.getenv("GITEA_USERNAME"), help="Gitea 账号，未传 token 时使用")
    parser.add_argument("--password", default=os.getenv("GITEA_PASSWORD"), help="Gitea 密码，未传 token 时使用")
    parser.add_argument("--timeout", type=int, default=DEFAULT_TIMEOUT)
    parser.add_argument("--json", action="store_true", help="以 JSON 格式输出结果")

    subparsers = parser.add_subparsers(dest="command", required=True)

    list_parser = subparsers.add_parser("list", help="查看 issue，可按标签过滤")
    list_parser.add_argument("--labels", help="逗号分隔的标签，例如 bug,frontend")
    list_parser.add_argument("--state", default="open", choices=["open", "closed", "all"])
    list_parser.add_argument("--page", type=int, default=1)
    list_parser.add_argument("--limit", type=int, default=20)

    get_parser = subparsers.add_parser("get", help="查看指定 issue")
    get_parser.add_argument("index", type=int, help="issue 编号")

    create_parser = subparsers.add_parser("create", help="添加工单 issue")
    create_parser.add_argument("title", help="工单标题")
    create_parser.add_argument("--body", help="工单内容")
    create_parser.add_argument("--labels", help="逗号分隔的标签")
    create_parser.add_argument("--assignees", help="逗号分隔的负责人用户名")
    create_parser.add_argument("--milestone", type=int, help="里程碑 ID")
    create_parser.add_argument("--due-date", help="截止时间，例如 2026-06-12T00:00:00+08:00")

    labels_parser = subparsers.add_parser("labels", help="查看仓库标签")
    labels_parser.set_defaults(command="labels")

    set_labels_parser = subparsers.add_parser("set-labels", help="覆盖指定 issue 的标签")
    set_labels_parser.add_argument("index", type=int, help="issue 编号")
    set_labels_parser.add_argument("labels", help="逗号分隔的标签")

    add_labels_parser = subparsers.add_parser("add-labels", help="给指定 issue 添加标签")
    add_labels_parser.add_argument("index", type=int, help="issue 编号")
    add_labels_parser.add_argument("labels", help="逗号分隔的标签")

    remove_label_parser = subparsers.add_parser("remove-label", help="删除指定 issue 的一个标签")
    remove_label_parser.add_argument("index", type=int, help="issue 编号")
    remove_label_parser.add_argument("label", help="标签名称或标签 ID")

    comment_parser = subparsers.add_parser("comment", help="评论指定 issue")
    comment_parser.add_argument("index", type=int, help="issue 编号")
    comment_parser.add_argument("body", help="评论内容")

    return parser


def run(args: argparse.Namespace) -> Any:
    manager = GiteaTechManager(
        base_url=args.base_url,
        owner=args.owner,
        repo=args.repo,
        token=args.token,
        username=args.username,
        password=args.password,
        timeout=args.timeout,
    )

    if args.command == "list":
        return manager.list_issues(
            labels=parse_labels(args.labels),
            state=args.state,
            page=args.page,
            limit=args.limit,
        )
    if args.command == "get":
        return manager.get_issue(args.index)
    if args.command == "create":
        return manager.create_issue(
            title=args.title,
            body=args.body,
            labels=parse_labels(args.labels),
            assignees=parse_labels(args.assignees),
            milestone=args.milestone,
            due_date=args.due_date,
        )
    if args.command == "labels":
        return manager.list_labels()
    if args.command == "set-labels":
        return manager.set_issue_labels(args.index, parse_labels(args.labels))
    if args.command == "add-labels":
        return manager.add_issue_labels(args.index, parse_labels(args.labels))
    if args.command == "remove-label":
        manager.remove_issue_label(args.index, args.label)
        return {"ok": True, "issue": args.index, "removed": args.label}
    if args.command == "comment":
        return manager.comment_issue(args.index, args.body)
    raise ValueError(f"未知命令: {args.command}")


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    try:
        result = run(args)
        if args.json or args.command != "list":
            print_json(result)
        else:
            print_issue_table(result)
        return 0
    except Exception as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
