<template>
  <el-dialog title="选择用户" v-model="visible" width="800px" top="5vh" append-to-body>
    <el-form :model="queryParams" ref="queryFormRef" :inline="true">
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
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row>
      <el-table
        @selection-change="handleSelectionChange"
        ref="tableRef"
        :data="userList"
        height="260px"
        border
      >
        <el-table-column type="selection" width="55"></el-table-column>
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
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.current"
        v-model:limit="queryParams.size"
        @pagination="getList"
      />
    </el-row>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleSelectUser">确 定</el-button>
        <el-button @click="visible = false">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { unallocatedUserList, authUserSelectAll } from '@/api/sys/role.ts';
import { ElMessage } from 'element-plus';
import Pagination from '@/components/Pagination/index.vue'
import { Search, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  roleId: {
    type: [Number, String],
    required: true
  }
});

const emit = defineEmits(['ok']);

const visible = ref(false);
const userList = ref([]);
const total = ref(0);
const userIds = ref<string[]>([]);
const queryParams = reactive({
  current: 1,
  size: 10,
  roleId: props.roleId,
  username: undefined,
  phone: undefined
});

const show = () => {
  queryParams.roleId = props.roleId;
  getList();
  visible.value = true;
};

const getList = () => {
  unallocatedUserList(queryParams).then((res: any) => {
    userList.value = res.records;
    total.value = res.total;
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
  userIds.value = selection.map((item) => item.userId);
};

const handleSelectUser = () => {
  const roleId = queryParams.roleId;
  const uIds = userIds.value.join(',');
  if (uIds == '') {
    ElMessage.error('请选择要分配的用户');
    return;
  }
  authUserSelectAll({ roleId: roleId, userIds: uIds }).then((res) => {
    ElMessage.success('分配成功');
    visible.value = false;
    emit('ok');
  });
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
