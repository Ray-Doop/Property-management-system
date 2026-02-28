<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-content">
        <h2 class="title">发布新公告</h2>
        <p class="subtitle">创建并发布通知给社区居民，支持富文本和附件上传</p>
      </div>
    </div>

    <el-card class="form-card" shadow="never">
      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-position="top"
        size="large"
      >
        <!-- 标题输入 -->
        <el-form-item label="公告标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入清晰明确的公告标题"
            maxlength="100"
            show-word-limit
          >
            <template #prefix>
              <el-icon><Document /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 发布设置 -->
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="发布时间" prop="publishTime">
              <el-date-picker
                v-model="form.publishTime"
                type="datetime"
                placeholder="立即发布（留空）或选择定时发布"
                style="width: 100%"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                :shortcuts="dateShortcuts"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可见范围" prop="targetResidenceId">
              <el-select
                v-model="form.targetResidenceId"
                placeholder="请选择可见范围（默认全员可见）"
                style="width: 100%"
                clearable
              >
                <el-option label="全员可见" value="" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 富文本编辑器 -->
        <el-form-item label="公告详情" prop="content">
          <div class="editor-container">
            <Toolbar
              style="border-bottom: 1px solid #e4e7ed"
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              :mode="mode"
            />
            <Editor
              style="height: 400px; overflow-y: hidden;"
              v-model="form.content"
              :defaultConfig="editorConfig"
              :mode="mode"
              @onCreated="handleCreated"
            />
          </div>
        </el-form-item>

        <!-- 附件上传 -->
        <el-form-item label="附件上传">
          <el-upload
            class="upload-area"
            drag
            :action="uploadUrl"
            :data="{ folder: 'notice' }"
            :headers="uploadHeaders"
            name="file"
            :on-success="handleUploadSuccess"
            :on-remove="handleRemove"
            :file-list="fileList"
            multiple
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持图片、PDF、Word等格式文件，单个文件不超过 50MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        
        <div class="form-actions">
          <div class="left-actions">
            <el-button @click="resetForm">重置表单</el-button>
          </div>
          <div class="right-actions">
            <el-button @click="saveDraft" :loading="loading" icon="EditPen">
              保存草稿
            </el-button>
            <el-button
              type="primary"
              @click="publish"
              :loading="loading"
              icon="Promotion"
              style="min-width: 120px"
            >
              正式发布
            </el-button>
          </div>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, shallowRef, onBeforeUnmount, computed } from "vue";
import { Editor, Toolbar } from "@wangeditor/editor-for-vue";
import "@wangeditor/editor/dist/css/style.css";
import request from "@/api/request.js";
import { ElMessage } from "element-plus";
import { Document, UploadFilled, EditPen, Promotion } from "@element-plus/icons-vue";


const loading = ref(false);
const formRef = ref(null);
const fileList = ref([]);
const attachments = ref([]);

const uploadUrl = "http://localhost:8080/files/upload";

const getUploadHeaders = () => {
  const user = JSON.parse(localStorage.getItem("code_user") || "{}");
  const token = user?.token || "";
  return token ? { Authorization: `Bearer ${token}` } : {};
};

const uploadHeaders = computed(() => getUploadHeaders());

const form = reactive({
  title: "",
  content: "",
  publishTime: "",
  targetResidenceId: "",
});

const rules = {
  title: [{ required: true, message: "请输入公告标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入公告内容", trigger: "blur" }],
};


const editorRef = shallowRef();
const mode = "default";
const toolbarConfig = {
  excludeKeys: ["group-video"]
};
const editorConfig = {
  placeholder: "请输入详细公告内容...",
  MENU_CONF: {
    uploadImage: {
      server: uploadUrl,
      headers: getUploadHeaders(),
      fieldName: "file",
      meta: { folder: "notice" },
      customInsert(res, insertFn) {
        if (res.code === "200") {
           insertFn(res.data, "", "");
        } else {
           insertFn(res.data, "", "");
        }
      },
    },
  },
};

// 销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value;
  if (editor == null) return;
  editor.destroy();
});

const handleCreated = (editor) => {
  editorRef.value = editor;
};

// --- 文件上传处理 ---
const handleUploadSuccess = (res, file, fileList) => {
  const url = res.data; 
  const fileType = getFileType(file.name);
  
  attachments.value.push({
    fileUrl: url,
    fileType: fileType,
    uid: file.uid
  });
  
  ElMessage.success(`文件 ${file.name} 上传成功`);
};

const handleRemove = (file) => {
  const index = attachments.value.findIndex(item => item.uid === file.uid);
  if (index !== -1) {
    attachments.value.splice(index, 1);
  }
};

const getFileType = (filename) => {
  const ext = filename.split('.').pop().toLowerCase();
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp'].includes(ext)) return 'image';
  if (['mp4', 'avi', 'mov'].includes(ext)) return 'video';
  return 'file';
};

// --- 表单提交 ---
const withAdminInfo = (payload) => {
  try {
    const admin = JSON.parse(localStorage.getItem("code_user") || "{}");
    payload.adminId = admin.adminId || admin.userId;
    payload.publisherName = admin.nickname || admin.username || "管理员";
  } catch (e) {
    console.error("获取用户信息失败", e);
  }
  return payload;
};

const submitForm = async (isDraft) => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const payload = withAdminInfo({
          ...form,
          attachments: attachments.value.map(({ uid, ...rest }) => rest)
        });

        const url = isDraft ? "/notice/saveDraft" : "/notice/publish";
        const res = await request.post(url, payload);

        if (res.code === "200") {
          ElMessage.success(isDraft ? "草稿已保存" : "公告发布成功");
          if (!isDraft) {
            resetForm();
          }
        } else {
          ElMessage.error(res.msg || (isDraft ? "保存失败" : "发布失败"));
        }
      } catch (error) {
        console.error(error);
        ElMessage.error("系统错误，请稍后重试");
      } finally {
        loading.value = false;
      }
    }
  });
};

const publish = () => submitForm(false);
const saveDraft = () => submitForm(true);

const resetForm = () => {
  formRef.value.resetFields();
  editorRef.value.clear();
  fileList.value = [];
  attachments.value = [];
};

// --- 工具 ---
const dateShortcuts = [
  {
    text: '今天',
    value: new Date(),
  },
  {
    text: '明天',
    value: () => {
      const date = new Date()
      date.setTime(date.getTime() + 3600 * 1000 * 24)
      return date
    },
  },
  {
    text: '一周后',
    value: () => {
      const date = new Date()
      date.setTime(date.getTime() + 3600 * 1000 * 24 * 7)
      return date
    },
  },
]
</script>

<style scoped>
.page-container {
  width: 100%;
  margin: 0;
  padding: 0;
}

.page-header {
  margin-bottom: 24px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0;
}

.subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 8px;
}

.form-card {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-color);
  padding: 32px;
}

.editor-container {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  margin-top: 8px;
}

.upload-area {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  height: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed var(--border-color);
  background: var(--bg-color);
  transition: all 0.3s;
}

:deep(.el-upload-dragger:hover) {
  border-color: var(--primary-color);
  background: var(--primary-subtle);
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--border-light);
}

.right-actions {
  display: flex;
  gap: 16px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--text-main);
  padding-bottom: 8px;
}
</style>
