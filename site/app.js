(function () {
    var translations = {
        zh: {
            page_title: 'PoolGuard | Java 线程池泄露扫描',
            page_desc: 'PoolGuard：面向 Java 项目的线程池泄露扫描 CLI 工具',
            nav_ai: 'AI 能力',
            nav_features: '能力',
            nav_rules: '规则',
            nav_workflow: '流程',
            nav_start: '开始',
            hero_title: '让线程池泄露问题<br />在提交前暴露',
            hero_desc: 'PoolGuard 是一个面向 Java 项目的 CLI 扫描工具，聚焦线程池生命周期风险，支持规则检测、LLM 复判与多格式报告输出。',
            hero_cta_primary: '立即试用',
            hero_cta_ai: '查看 AI 能力',
            meta_java: '兼容运行',
            meta_rules: '核心规则',
            meta_ai: 'AI 复判闭环',
            ai_kicker: 'AI 审核引擎',
            ai_title: '规则检测之后，再做一轮语义复判',
            ai_card1_title: '误报收敛',
            ai_card1_desc: '通过代码上下文理解，降低规则误报，帮助团队优先处理真正高风险问题。',
            ai_card2_title: '可控降级',
            ai_card2_desc: '当 AI 调用超时、限流或失败时，自动回退规则结果，保证扫描流程稳定。',
            ai_card3_title: '并发可调',
            ai_card3_desc: '通过 `--llm-concurrency` 控制复判并发，平衡速度与配额成本。',
            ai_card4_title: '模型可配',
            ai_card4_desc: '默认 `glm-5`，可通过 `--llm-model` 切换模型，匹配企业策略。',
            features_kicker: '核心能力',
            features_title: '为 CI 与本地扫描设计',
            f1_title: '全量 + 增量扫描',
            f1_desc: '支持 `--changed-files`，只扫描变更与扩散影响文件，降低扫描成本。',
            f2_title: 'AI 语义复判',
            f2_desc: '可启用 DashScope 复判，失败自动降级规则结果，不阻断流程。',
            f3_title: '多格式报告',
            f3_desc: '提供 JSON、Markdown、Text 三种输出，便于接入平台或直接阅读。',
            f4_title: '缓存机制',
            f4_desc: '基于文件哈希缓存减少重复解析，提高迭代场景下执行效率。',
            rules_kicker: '规则覆盖',
            rules_title: '首期 P0 风险规则',
            r1: 'PG001 高频入口方法内创建线程池',
            r2: 'PG002 循环/递归重复创建线程池',
            r3: 'PG003 未关闭或关闭路径不完整',
            r4: 'PG005 生命周期不匹配',
            r5: 'PG006 默认线程工厂不可观测',
            r6: 'PG007 无界队列风险',
            r7: 'PG008 定时线程池未取消且未关闭',
            r8: 'PG009 静态线程池缺少退出钩子',
            flow_kicker: '执行路径',
            flow_title: '扫描流程',
            flow_1: '参数校验与扫描模式判定（全量 / 增量）',
            flow_2: 'Java 文件发现与源码解析，提取线程池事实模型',
            flow_3: '规则引擎执行 PG001~PG009',
            flow_4: '按需触发 LLM 复判并合并结果',
            flow_5: '按排序策略输出报告到 stdout 或文件',
            arch_kicker: '组件分层',
            arch_title: '架构概览',
            start_kicker: '快速开始',
            start_title: '本地两步运行',
            footer_text: 'PoolGuard · Thread Pool Leak Scanner',
            back_to_top: '回到顶部'
        },
        en: {
            page_title: 'PoolGuard | Java Thread Pool Leak Scanner',
            page_desc: 'PoolGuard: a CLI tool for detecting thread-pool leak risks in Java projects',
            nav_ai: 'AI',
            nav_features: 'Features',
            nav_rules: 'Rules',
            nav_workflow: 'Workflow',
            nav_start: 'Start',
            hero_title: 'Expose thread-pool leaks<br />before they reach production',
            hero_desc: 'PoolGuard is a CLI scanner for Java projects, focused on thread-pool lifecycle risks with rule checks, LLM re-evaluation, and multi-format reports.',
            hero_cta_primary: 'Try Now',
            hero_cta_ai: 'See AI Capabilities',
            meta_java: 'Runtime Compatible',
            meta_rules: 'Core Rules',
            meta_ai: 'AI Review Loop',
            ai_kicker: 'AI Review Engine',
            ai_title: 'Semantic re-evaluation after rule detection',
            ai_card1_title: 'False Positive Reduction',
            ai_card1_desc: 'Use contextual code understanding to reduce noisy findings and prioritize truly risky issues.',
            ai_card2_title: 'Controlled Fallback',
            ai_card2_desc: 'If AI calls timeout, throttle, or fail, results automatically fall back to rule output to keep pipelines stable.',
            ai_card3_title: 'Tunable Concurrency',
            ai_card3_desc: 'Adjust review throughput with `--llm-concurrency` to balance speed and quota cost.',
            ai_card4_title: 'Configurable Model',
            ai_card4_desc: 'Default model is `glm-5`; switch via `--llm-model` to fit enterprise standards.',
            features_kicker: 'Core Capabilities',
            features_title: 'Built for CI and local workflows',
            f1_title: 'Full + Incremental Scan',
            f1_desc: 'With `--changed-files`, scan only changed and impacted files to cut execution cost.',
            f2_title: 'AI Semantic Review',
            f2_desc: 'Enable DashScope re-evaluation with automatic downgrade on failure, without breaking pipeline execution.',
            f3_title: 'Multi-format Reports',
            f3_desc: 'Generate JSON, Markdown, and Text output for integrations or direct reading.',
            f4_title: 'Cache Mechanism',
            f4_desc: 'File-hash cache reduces duplicate parsing and improves iteration speed.',
            rules_kicker: 'Rule Coverage',
            rules_title: 'Initial P0 risk rules',
            r1: 'PG001 Create thread pool in high-frequency entry methods',
            r2: 'PG002 Recreate thread pool in loops/recursion',
            r3: 'PG003 Missing shutdown or incomplete close path',
            r4: 'PG005 Lifecycle mismatch',
            r5: 'PG006 Unobservable default thread factory',
            r6: 'PG007 Unbounded queue risk',
            r7: 'PG008 Scheduled pool not cancelled and not shutdown',
            r8: 'PG009 Static pool missing shutdown hook',
            flow_kicker: 'Execution Path',
            flow_title: 'Scan workflow',
            flow_1: 'Validate arguments and detect scan mode (full/incremental)',
            flow_2: 'Discover Java files and parse facts for thread-pool usage',
            flow_3: 'Run PG001~PG009 rule engine',
            flow_4: 'Trigger LLM re-evaluation when enabled and merge results',
            flow_5: 'Sort and output report to stdout or file',
            arch_kicker: 'Layered Components',
            arch_title: 'Architecture Overview',
            start_kicker: 'Quick Start',
            start_title: 'Run locally in two steps',
            footer_text: 'PoolGuard · Thread Pool Leak Scanner',
            back_to_top: 'Back to Top'
        }
    };

    function applyLanguage(lang) {
        var dict = translations[lang] || translations.zh;
        var nodes = document.querySelectorAll('[data-i18n]');
        var htmlNodes = document.querySelectorAll('[data-i18n-html]');

        for (var i = 0; i < nodes.length; i += 1) {
            var key = nodes[i].getAttribute('data-i18n');
            if (dict[key]) {
                nodes[i].textContent = dict[key];
            }
        }

        for (var j = 0; j < htmlNodes.length; j += 1) {
            var htmlKey = htmlNodes[j].getAttribute('data-i18n-html');
            if (dict[htmlKey]) {
                htmlNodes[j].innerHTML = dict[htmlKey];
            }
        }

        document.documentElement.lang = lang === 'en' ? 'en' : 'zh-CN';
        if (dict.page_title) {
            document.title = dict.page_title;
        }
        if (dict.page_desc) {
            var metaDescription = document.querySelector('meta[name=\"description\"]');
            if (metaDescription) {
                metaDescription.setAttribute('content', dict.page_desc);
            }
        }
        try {
            localStorage.setItem('poolguard_lang', lang);
        } catch (e) {
            // ignore storage failures
        }

        var langButtons = document.querySelectorAll('.lang-btn');
        for (var k = 0; k < langButtons.length; k += 1) {
            var active = langButtons[k].getAttribute('data-lang') === lang;
            if (active) {
                langButtons[k].classList.add('is-active');
            } else {
                langButtons[k].classList.remove('is-active');
            }
        }
    }

    function bindLanguageSwitcher() {
        var buttons = document.querySelectorAll('.lang-btn');
        for (var i = 0; i < buttons.length; i += 1) {
            buttons[i].addEventListener('click', function (event) {
                var nextLang = event.currentTarget.getAttribute('data-lang');
                applyLanguage(nextLang);
            });
        }

        var defaultLang = 'zh';
        try {
            var storedLang = localStorage.getItem('poolguard_lang');
            if (storedLang === 'en' || storedLang === 'zh') {
                defaultLang = storedLang;
            }
        } catch (e) {
            // ignore storage failures
        }
        applyLanguage(defaultLang);
    }

    function bindRevealAnimation() {
        var nodes = document.querySelectorAll('.reveal');
        if (!('IntersectionObserver' in window) || nodes.length === 0) {
            return;
        }

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add('in');
                    observer.unobserve(entry.target);
                }
            });
        }, {
            threshold: 0.18,
            rootMargin: '0px 0px -40px 0px'
        });

        for (var i = 0; i < nodes.length; i += 1) {
            observer.observe(nodes[i]);
        }
    }

    function initMermaid() {
        if (!window.mermaid) {
            return;
        }
        window.mermaid.initialize({
            startOnLoad: false,
            theme: 'dark',
            securityLevel: 'loose'
        });
        window.mermaid.run({
            querySelector: '.mermaid'
        });
    }

    bindLanguageSwitcher();
    bindRevealAnimation();
    initMermaid();
})();
