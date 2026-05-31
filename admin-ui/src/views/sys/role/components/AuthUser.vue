<template>
  <el-dialog title="分配用户" v-model="visible" width="1000px" top="5vh" append-to-body class="auth-user-dialog">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" class="search-form">
      <el-form-item label="用户名称" prop="username">
        <el-input
          v-model="queryParams.username"
          placeholder="请输入用户名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号码" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入手机号码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="openSelectUser"
        >添加用户</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="CircleClose"
          :disabled="multiple"
          @click="cancelAuthUserAll"
        >批量取消授权</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Close"
          @click="handleClose"
        >关闭</el-button>
      </el-col>
    </el-row>

    <el-table
      v-loading="loading"
      :data="userList"
      @selection-change="handleSelectionChange"
      height="400px"
      border
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户名称" prop="username" :show-overflow-tooltip="true" />
      <el-table-column label="用户昵称" prop="nickname" :show-overflow-tooltip="true" />
      <el-table-column label="邮箱" prop="email" :show-overflow-tooltip="true" />
      <el-table-column label="手机" prop="phone" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1">正常</el-tag>
          <el-tag v-else type="danger">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ scope.row.createTime }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="CircleClose"
            @click="cancelAuthUser(scope.row)"
          >取消授权</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.current"
      v-model:limit="queryParams.size"
      @pagination="getList"
    />

    <select-user ref="selectUserRef" :role-id="queryParams.roleId" @ok="handleQuery" />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { allocatedUserList, authUserCancel, authUserCancelAll } from '@/api/sys/role.ts';
import SelectUser from './SelectUser.vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import Pagination from '@/components/Pagination/index.vue'

const visible = ref(false);
const loading = ref(false);
const userList = ref([]);
const total = ref(0);
const userIds = ref<string[]>([]);
const multiple = ref(true);
const selectUserRef = ref();

const queryParams = reactive({
  current: 1,
  size: 10,
  roleId: '' as string | number,
  username: undefined,
  phone: undefined
});

const show = (roleId: string | number) => {
  queryParams.roleId = roleId;
  getList();
  visible.value = true;
};

const handleClose = () => {
  visible.value = false;
};

const getList = () => {
  loading.value = true;
  allocatedUserList(queryParams).then((res: any) => {
    userList.value = res.records;
    total.value = res.total;
    loading.value = false;
  });
};

const handleQuery = () => {
  queryParams.current = 1;
  getList();
};

const resetQuery = () => {
  queryParams.username = undefined;
  queryParams.phone = undefined;
  handleQuery();
};

const handleSelectionChange = (selection: any[]) => {
  userIds.value = selection.map((item) => item.id);
  multiple.value = !selection.length;
};

const openSelectUser = () => {
  selectUserRef.value.show();
};

const cancelAuthUser = (row: any) => {
  ElMessageBox.confirm('确认要取消该用户"' + row.username + '"角色吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return authUserCancel({ roleId: queryParams.roleId, userId: row.roleId });
  }).then(() => {
    getList();
    ElMessage.success('取消授权成功');
  }).catch(() => {});
};

const cancelAuthUserAll = () => {
  const uIds = userIds.value.join(',');
  ElMessageBox.confirm('是否取消选中用户的授权数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return authUserCancelAll({ roleId: queryParams.roleId, userIds: uIds });
  }).then(() => {
    getList();
    ElMessage.success('取消授权成功');
  }).catch(() => {});
};

defineExpose({
  show
});
</script>

<style scoped>
:deep(.el-table__cell) {
  padding: 20px 0;
}
</style>

<style>
.auth-user-dialog .el-dialog__header {
  border-bottom: none !important;
  margin-right: 0;
}
.auth-user-dialog .el-dialog__body {
  padding-top: 10px !important;
}
</style>

<style scoped>
.search-form {
  margin-bottom: 10px;
}
.mb8 {
  margin-bottom: 20px;
}

:deep(.el-table__cell) {
  padding: 20px 0;
}
</style>
