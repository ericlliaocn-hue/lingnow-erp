<template>
  <div class="app-container bill-form-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="bill-form-title">
            <strong>{{ title }}</strong>
            <span v-if="saleDraftEnabled && draftSaveStatusText" class="draft-save-status" :class="`is-${draftSaveStatus}`">{{ draftSaveStatusText }}</span>
          </div>
          <div class="card-actions">
            <el-popover v-if="saleDraftEnabled" placement="bottom-end" width="360" trigger="click" @show="loadSaleDraftMeta">
              <template #reference>
                <el-button :icon="FolderChecked" :type="saleDraftAvailable ? 'warning' : ''">
                  <span>草稿箱</span>
                  <span v-if="saleDraftAvailable" class="draft-count">1</span>
                </el-button>
              </template>
              <div class="sale-draft-popover">
                <div class="sale-draft-head">
                  <strong>销售单草稿</strong>
                  <span>{{ saleDraftAvailable ? saleDraftSavedAtText : '暂无草稿' }}</span>
                </div>
                <template v-if="saleDraftAvailable">
                  <div class="sale-draft-summary">
                    <div>
                      <span>客户</span>
                      <strong>{{ saleDraftSummary.partner }}</strong>
                    </div>
                    <div>
                      <span>收货人</span>
                      <strong>{{ saleDraftSummary.receiver }}</strong>
                    </div>
                    <div>
                      <span>商品</span>
                      <strong>{{ saleDraftSummary.itemCount }} 件</strong>
                    </div>
                    <div>
                      <span>LOGO</span>
                      <strong>{{ saleDraftSummary.logoCount }} 张</strong>
                    </div>
                  </div>
                  <div class="sale-draft-actions">
                    <el-button type="primary" :icon="RefreshRight" @click="restoreSaleDraftFromBox">恢复草稿</el-button>
                    <el-button :icon="Delete" @click="clearSaleDraftFromBox">清空草稿</el-button>
                  </div>
                </template>
                <el-empty v-else description="暂无草稿" :image-size="64" />
              </div>
            </el-popover>
            <el-button @click="back">返回</el-button>
            <el-button v-if="showSaveButton" type="primary" @click="submit" v-permission="savePermission">保存</el-button>
          </div>
        </div>
      </template>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col v-if="showEmployeeField" :span="6">
            <el-form-item label="业务员">
              <el-select
                v-if="canManageEmployee"
                v-model="employeeSelectValue"
                :disabled="readonly"
                filterable
                style="width: 100%"
                placeholder="请选择业务员"
              >
                <el-option v-for="item in employeeSelectOptions" :key="item.userId" :label="employeeLabel(item)" :value="item.userId">
                  <div class="employee-option">
                    <span class="employee-option-name">{{ employeeLabel(item) }}</span>
                    <el-button
                      link
                      type="danger"
                      :icon="Minus"
                      :loading="employeeDisableLoadingId === String(item.userId)"
                      :disabled="employeeDisableLoadingId === String(item.userId)"
                      title="禁用业务员"
                      @mousedown.stop.prevent
                      @click.stop.prevent="disableEmployee(item)"
                    />
                  </div>
                </el-option>
                <template #empty>
                  <div class="select-empty-action">
                    <span>暂无业务员</span>
                    <el-button link type="primary" :icon="Plus" :disabled="readonly" @click.stop="openQuickEmployee">新增业务员</el-button>
                  </div>
                </template>
                <template #footer>
                  <el-button link type="primary" :icon="Plus" :disabled="readonly" @click.stop="openQuickEmployee">新增业务员</el-button>
                </template>
              </el-select>
              <el-input v-else v-model="form.employeeName" disabled placeholder="自动取当前登录账号" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="6"><el-form-item label="单号"><el-input v-model="form.billNo" :disabled="readonly" placeholder="自动生成" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="日期" prop="billDate"><el-date-picker v-model="form.billDate" :disabled="readonly" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6">
            <el-form-item :label="partnerLabel" prop="partnerId">
              <el-select v-model="form.partnerId" :disabled="readonly" filterable style="width: 100%" :placeholder="`请选择${partnerLabel}`">
                <el-option v-for="item in partners" :key="item.id" :label="item.name" :value="item.id" />
                <template v-if="showReceiver" #empty>
                  <div class="select-empty-action">
                    <span>暂无客户</span>
                    <el-button link type="primary" :icon="Plus" :disabled="readonly" @click.stop="openQuickCustomer">新增客户</el-button>
                  </div>
                </template>
                <template v-if="showReceiver" #footer>
                  <el-button link type="primary" :icon="Plus" :disabled="readonly" @click.stop="openQuickCustomer">新增客户</el-button>
                </template>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" :disabled="readonly" filterable style="width: 100%" placeholder="请选择仓库">
                <el-option v-for="item in warehouses" :key="item.id" :label="item.name" :value="item.id" />
                <template #empty>
                  <div class="select-empty-action">
                    <span>暂无仓库</span>
                    <el-button link type="primary" :icon="Plus" :disabled="readonly" @click.stop="openQuickWarehouse">新增仓库</el-button>
                  </div>
                </template>
                <template #footer>
                  <el-button link type="primary" :icon="Plus" :disabled="readonly" @click.stop="openQuickWarehouse">新增仓库</el-button>
                </template>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6"><el-form-item label="账户" prop="accountId" :required="paymentRequired"><el-select v-model="form.accountId" :disabled="readonly" clearable filterable style="width: 100%"><el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col>
          <el-col :span="6">
            <el-form-item label="付款方式" prop="paymentMethod" :required="paymentRequired">
              <el-select v-model="form.paymentMethod" :disabled="readonly" clearable style="width: 100%" placeholder="请选择付款方式">
                <el-option v-for="item in paymentMethods" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6"><el-form-item label="付款金额"><el-input-number v-model="form.paidAmount" :disabled="readonly" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="整单优惠"><el-input-number v-model="form.discountAmount" :disabled="readonly" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="其他费用"><el-input-number v-model="form.otherAmount" :disabled="readonly" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <template v-if="showReceiver">
          <el-divider />
          <div class="section-title">
            <strong>收货信息</strong>
            <el-button :icon="Aim" :disabled="readonly" @click="openAddressParse">地址识别</el-button>
          </div>
          <el-row :gutter="16">
            <el-col :span="6"><el-form-item label="收货人"><el-input v-model="form.receiverName" :disabled="readonly" placeholder="请输入收货人" /></el-form-item></el-col>
            <el-col :span="6"><el-form-item label="收货电话"><el-input v-model="form.receiverPhone" :disabled="readonly" placeholder="请输入收货电话" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="收货地址"><el-input v-model="form.receiverAddress" :disabled="readonly" placeholder="请输入收货地址" /></el-form-item></el-col>
          </el-row>
        </template>
        <template v-if="isProduction">
          <el-divider />
          <div class="section-title">
            <strong>生产信息</strong>
          </div>
          <el-row :gutter="16">
            <el-col :span="8"><el-form-item label="生产进度"><el-input v-model="form.productionProgress" :disabled="!productionEditable" placeholder="请输入生产进度" maxlength="64" /></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="快递单号"><el-input v-model="form.trackingNo" :disabled="!productionEditable" placeholder="请输入快递单号" maxlength="100" /></el-form-item></el-col>
            <el-col :span="8">
              <el-form-item label="生产人员">
                <el-input v-model="form.productionUserName" :disabled="!productionEditable" placeholder="请输入生产人员" maxlength="64" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <el-divider />
        <div class="toolbar"><el-button type="primary" :disabled="readonly" @click="addRow">添加商品</el-button></div>
        <div class="bill-item-list">
          <div v-for="(row, index) in form.items" :key="row.id || index" class="bill-item-card">
            <div class="bill-item-head">
              <span>商品 {{ index + 1 }}</span>
              <el-button link type="primary" :disabled="readonly" @click="removeRow(index)">删除</el-button>
            </div>
            <div class="bill-item-product">
              <el-image
                v-if="row.productImageUrl"
                class="bill-product-image"
                :src="row.productImageUrl"
                :preview-src-list="[row.productImageUrl]"
                preview-teleported
                fit="cover"
              />
              <span v-else class="bill-product-placeholder">无图</span>
              <el-form-item label="商品" class="bill-item-product-select" :prop="`items.${index}.productId`" :rules="itemProductRules">
                <el-select
                  v-model="row.productId"
                  filterable
                  remote
                  reserve-keyword
                  :remote-method="(keyword: string) => loadRowProducts(row, keyword)"
                  placeholder="搜索商品编号 / 名称 / 条码"
                  :disabled="readonly"
                  style="width: 100%"
                  @visible-change="(visible: boolean) => visible && loadRowProducts(row)"
                  @change="productChanged(row)"
                >
                  <el-option v-for="item in rowProductOptions(row)" :key="item.id" :label="`${item.code} ${item.name}`" :value="item.id">
                    <div class="product-option">
                      <img v-if="item.imageUrl" :src="item.imageUrl" class="product-option-image" />
                      <span v-else class="product-option-empty">无图</span>
                      <div class="product-option-text">
                        <strong>{{ item.code }} {{ item.name }}</strong>
                        <span>{{ [item.spec, item.attributeText].filter(Boolean).join(' / ') || '暂无规格属性' }}</span>
                      </div>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </div>
            <el-row :gutter="12">
              <el-col :span="showCostPrice ? 5 : 6"><el-form-item label="规格"><el-input v-model="row.spec" disabled placeholder="自动带出" /></el-form-item></el-col>
              <el-col :span="showCostPrice ? 5 : 6"><el-form-item label="数量" :prop="`items.${index}.qty`" :rules="itemQtyRules"><el-input-number v-model="row.qty" :disabled="readonly" :min="0" :precision="2" style="width: 100%" @change="calc" /></el-form-item></el-col>
              <el-col v-if="showCostPrice" :span="4"><el-form-item label="成本价"><el-input :model-value="costPriceText(row)" disabled /></el-form-item></el-col>
              <el-col :span="showCostPrice ? 5 : 6">
                <el-form-item label="单价">
                  <el-input-number v-if="canEditLineAmount" v-model="row.price" :disabled="readonly" :min="0" :precision="2" style="width: 100%" @change="calc" />
                  <el-input v-else :model-value="''" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="showCostPrice ? 5 : 6"><el-form-item label="金额"><el-input :model-value="lineAmountText(row)" disabled /></el-form-item></el-col>
              <el-col :span="24" v-if="rowAttributeGroups(row).length">
                <el-form-item label="商品属性">
                  <div class="attribute-group-grid">
                    <div v-for="group in rowAttributeGroups(row)" :key="group.id" class="attribute-group-field">
                      <span class="attribute-group-label">{{ group.name }}</span>
                      <el-select
                        v-model="row.attributeSelections![String(group.id)]"
                        clearable
                        filterable
                        :disabled="readonly"
                        :placeholder="`请选择${group.name}`"
                        @change="attributeSelectionChanged(row)"
                      >
                        <el-option v-for="item in attributeOptions(String(group.id))" :key="item.id" :label="attributeOptionLabel(item)" :value="String(item.id)" />
                      </el-select>
                    </div>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="定制说明">
                  <el-input v-model="row.remark" :disabled="readonly" placeholder="例如：logo:ZHENYOUFAN 填白色，衣架/裤架居中" @input="syncRowSnapshot(row)" />
                </el-form-item>
              </el-col>
              <el-col v-if="showLogoUpload" :span="24">
                <el-form-item label="LOGO图片">
                  <div class="bill-logo-field">
                    <el-upload
                      class="bill-logo-uploader"
                      accept="image/*"
                      :show-file-list="false"
                      :disabled="readonly"
                      :http-request="(options: any) => uploadLogoImage(row, options)"
                    >
                      <div class="bill-logo-box" :class="{ 'has-image': !!row.logoImageUrl }">
                        <img v-if="row.logoImageUrl" :src="row.logoImageUrl" class="bill-logo-preview" />
                        <div v-else class="bill-logo-placeholder">
                          <el-icon><UploadFilled /></el-icon>
                          <span>上传LOGO</span>
                        </div>
                      </div>
                    </el-upload>
                    <el-button v-if="row.logoImageUrl && !readonly" link type="danger" @click="clearLogoImage(row)">删除</el-button>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
            <div v-if="rowLinePath(row)" class="bill-item-path">{{ rowLinePath(row) }}</div>
          </div>
          <el-empty v-if="!form.items.length" description="暂无商品明细" />
        </div>
        <div class="totals">
          <span>总数量：{{ totalQty }}</span>
          <span>成本：{{ financialSummaryText(costAmount) }}</span>
          <span>售价：{{ financialSummaryText(totalAmount) }}</span>
          <span>付款价格：{{ financialSummaryText(Number(form.paidAmount || 0).toFixed(2)) }}</span>
          <span>利润：{{ financialSummaryText(profitAmount) }}</span>
        </div>
        <el-form-item :label="billRemarkLabel"><el-input v-model="form.remark" :disabled="readonly" type="textarea" /></el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="addressOpen" title="地址识别" width="640px" append-to-body>
      <el-form label-width="92px">
        <el-form-item label="粘贴内容">
          <el-input v-model="addressRawText" type="textarea" :rows="5" placeholder="例如：李 15588937977 山东省济南市莱芜区万福北路吕花园沿街楼08号 学思途教育" />
        </el-form-item>
        <div class="address-parse-result">
          <div v-if="addressResult" class="address-parse-tip">请检查拆分后的收货人、电话、地址是否准确。</div>
          <div class="address-contact-line">
            <el-input v-model="addressEditForm.contactName" placeholder="收货人" />
            <div class="address-phone-field">
              <el-input v-model="addressEditForm.phone" placeholder="收货电话" />
              <span v-if="isVirtualPhone(addressEditForm.phone)" class="address-phone-hint">当前为虚拟号电话</span>
            </div>
          </div>
          <div v-if="contactCandidateOptions(addressResult).length > 1" class="address-candidate-box">
            <span>收货人可能是</span>
            <el-radio-group v-model="addressEditForm.contactName">
              <el-radio-button v-for="item in contactCandidateOptions(addressResult)" :key="item" :label="item" :value="item" />
            </el-radio-group>
          </div>
          <el-popover
            v-model:visible="addressRegionPickerOpen"
            placement="bottom-start"
            trigger="click"
            :width="620"
            popper-class="address-region-popper"
            @show="openAddressRegionPicker('sale')"
          >
            <template #reference>
              <el-input
                :model-value="addressRegionSelectionText(addressEditForm)"
                readonly
                clearable
                class="address-region-picker-input"
                placeholder="搜索或选择省 / 市 / 区县 / 镇街 / 村社区"
                :disabled="addressAreaLoading"
                @clear="clearAddressRegion('sale', addressEditForm)"
              />
            </template>
            <div class="address-region-picker">
              <el-input
                v-model="addressRegionSearchKeyword"
                clearable
                placeholder="搜索省市区县镇街村"
                @input="handleAddressRegionSearch('sale')"
                @clear="clearAddressRegionSearch('sale')"
              />
              <div v-if="addressRegionSearchKeyword.trim()" class="address-region-search-results">
                <div v-if="addressRegionSearchLoading" class="address-region-empty">搜索中...</div>
                <template v-else>
                  <button
                    v-for="item in addressRegionSearchResults"
                    :key="item.code"
                    type="button"
                    class="address-region-result"
                    @click="selectAddressRegionSearchResult('sale', addressEditForm, item, true)"
                  >
                    {{ addressRegionOptionPathText(item) }}
                  </button>
                  <div v-if="!addressRegionSearchResults.length" class="address-region-empty">没有匹配地址</div>
                </template>
              </div>
              <div v-else class="address-region-columns">
                <div v-for="(column, columnIndex) in addressRegionColumns(addressRegionPath)" :key="columnIndex" class="address-region-column">
                  <button
                    v-for="item in column"
                    :key="item.code"
                    type="button"
                    class="address-region-option"
                    :class="{ active: addressRegionPath[columnIndex] === item.code }"
                    @click="selectAddressRegionOption('sale', addressEditForm, columnIndex, item, true)"
                  >
                    {{ item.name }}
                  </button>
                </div>
              </div>
            </div>
          </el-popover>
          <el-input v-model="addressEditForm.detailAddress" type="textarea" :rows="3" placeholder="门牌号、楼栋、公司、房间号等详细地址" />
          <div class="address-normalized-preview">{{ addressFullText(addressEditForm) || '-' }}</div>
        </div>
        <el-alert v-if="addressResult?.warnings?.length" :title="addressResult.warnings.join('，')" type="warning" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="addressOpen = false">取消</el-button>
        <el-button :loading="addressLoading" @click="doParseAddress">识别</el-button>
        <el-button type="primary" :disabled="!hasAddressEditValue(addressEditForm)" @click="applyAddress">确认回填</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quickCustomerOpen" title="新增客户" width="720px" append-to-body destroy-on-close>
      <el-form ref="quickCustomerFormRef" :model="quickCustomerForm" :rules="quickCustomerRules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="编码" prop="code">
              <el-input v-model="quickCustomerForm.code" placeholder="请输入客户编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contact">
              <el-input v-model="quickCustomerForm.contact" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="quickCustomerForm.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址" prop="address">
              <el-input v-model="quickCustomerForm.address" placeholder="请输入地址">
                <template #append>
                  <el-button :icon="Aim" @click="openQuickCustomerAddressParse">识别</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="quickCustomerForm.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="quickCustomerForm.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="quickCustomerForm.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="quickCustomerOpen = false">取消</el-button>
        <el-button type="primary" :loading="quickCustomerSaving" @click="submitQuickCustomer">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quickCustomerAddressOpen" title="地址识别" width="640px" append-to-body>
      <el-form label-width="92px">
        <el-form-item label="粘贴内容">
          <el-input v-model="quickCustomerAddressRawText" type="textarea" :rows="5" placeholder="例如：李 15588937977 山东省济南市莱芜区万福北路吕花园沿街楼08号 学思途教育" />
        </el-form-item>
        <div class="address-parse-result">
          <div v-if="quickCustomerAddressResult" class="address-parse-tip">请检查拆分后的联系人、电话、地址是否准确。</div>
          <div class="address-contact-line">
            <el-input v-model="quickCustomerAddressEditForm.contactName" placeholder="联系人" />
            <div class="address-phone-field">
              <el-input v-model="quickCustomerAddressEditForm.phone" placeholder="联系电话" />
              <span v-if="isVirtualPhone(quickCustomerAddressEditForm.phone)" class="address-phone-hint">当前为虚拟号电话</span>
            </div>
          </div>
          <div v-if="contactCandidateOptions(quickCustomerAddressResult).length > 1" class="address-candidate-box">
            <span>联系人可能是</span>
            <el-radio-group v-model="quickCustomerAddressEditForm.contactName">
              <el-radio-button v-for="item in contactCandidateOptions(quickCustomerAddressResult)" :key="item" :label="item" :value="item" />
            </el-radio-group>
          </div>
          <el-popover
            v-model:visible="quickCustomerAddressRegionPickerOpen"
            placement="bottom-start"
            trigger="click"
            :width="620"
            popper-class="address-region-popper"
            @show="openAddressRegionPicker('quickCustomer')"
          >
            <template #reference>
              <el-input
                :model-value="addressRegionSelectionText(quickCustomerAddressEditForm)"
                readonly
                clearable
                class="address-region-picker-input"
                placeholder="搜索或选择省 / 市 / 区县 / 镇街 / 村社区"
                :disabled="addressAreaLoading"
                @clear="clearAddressRegion('quickCustomer', quickCustomerAddressEditForm)"
              />
            </template>
            <div class="address-region-picker">
              <el-input
                v-model="quickCustomerAddressRegionSearchKeyword"
                clearable
                placeholder="搜索省市区县镇街村"
                @input="handleAddressRegionSearch('quickCustomer')"
                @clear="clearAddressRegionSearch('quickCustomer')"
              />
              <div v-if="quickCustomerAddressRegionSearchKeyword.trim()" class="address-region-search-results">
                <div v-if="quickCustomerAddressRegionSearchLoading" class="address-region-empty">搜索中...</div>
                <template v-else>
                  <button
                    v-for="item in quickCustomerAddressRegionSearchResults"
                    :key="item.code"
                    type="button"
                    class="address-region-result"
                    @click="selectAddressRegionSearchResult('quickCustomer', quickCustomerAddressEditForm, item, true)"
                  >
                    {{ addressRegionOptionPathText(item) }}
                  </button>
                  <div v-if="!quickCustomerAddressRegionSearchResults.length" class="address-region-empty">没有匹配地址</div>
                </template>
              </div>
              <div v-else class="address-region-columns">
                <div v-for="(column, columnIndex) in addressRegionColumns(quickCustomerAddressRegionPath)" :key="columnIndex" class="address-region-column">
                  <button
                    v-for="item in column"
                    :key="item.code"
                    type="button"
                    class="address-region-option"
                    :class="{ active: quickCustomerAddressRegionPath[columnIndex] === item.code }"
                    @click="selectAddressRegionOption('quickCustomer', quickCustomerAddressEditForm, columnIndex, item, true)"
                  >
                    {{ item.name }}
                  </button>
                </div>
              </div>
            </div>
          </el-popover>
          <el-input v-model="quickCustomerAddressEditForm.detailAddress" type="textarea" :rows="3" placeholder="门牌号、楼栋、公司、房间号等详细地址" />
          <div class="address-normalized-preview">{{ addressFullText(quickCustomerAddressEditForm) || '-' }}</div>
        </div>
        <el-alert v-if="quickCustomerAddressResult?.warnings?.length" :title="quickCustomerAddressResult.warnings.join('，')" type="warning" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="quickCustomerAddressOpen = false">取消</el-button>
        <el-button :loading="quickCustomerAddressLoading" @click="doParseQuickCustomerAddress">识别</el-button>
        <el-button type="primary" :disabled="!hasAddressEditValue(quickCustomerAddressEditForm)" @click="applyQuickCustomerAddress">确认回填</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quickWarehouseOpen" title="新增仓库" width="420px" append-to-body destroy-on-close>
      <el-form ref="quickWarehouseFormRef" :model="quickWarehouseForm" :rules="quickWarehouseRules" label-width="76px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="quickWarehouseForm.name" placeholder="请输入仓库名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickWarehouseOpen = false">取消</el-button>
        <el-button type="primary" :loading="quickWarehouseSaving" @click="submitQuickWarehouse">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quickEmployeeOpen" title="新增业务员" width="420px" append-to-body destroy-on-close>
      <el-form ref="quickEmployeeFormRef" :model="quickEmployeeForm" :rules="quickEmployeeRules" label-width="76px">
        <el-form-item label="姓名" prop="nickname">
          <el-input v-model="quickEmployeeForm.nickname" placeholder="请输入业务员姓名" maxlength="30" />
        </el-form-item>
        <el-form-item label="账号" prop="username">
          <el-input v-model="quickEmployeeForm.username" placeholder="请输入登录账号" maxlength="30" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="quickEmployeeForm.password" placeholder="请输入初始密码" type="password" maxlength="20" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickEmployeeOpen = false">取消</el-button>
        <el-button type="primary" :loading="quickEmployeeSaving" @click="submitQuickEmployee">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, toRefs, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Aim, Delete, FolderChecked, Minus, Plus, RefreshRight, UploadFilled } from '@element-plus/icons-vue'
import { addBill, getBill, nextBillNo, updateBill, updateProductionBill, type BillItem, type BillModule, type ErpBill } from '@/api/erp/bill'
import { getProduct, productOptions, type ErpProduct } from '@/api/erp/product'
import { addMaster, listMaster, type ErpMasterForm, type ErpMasterVO } from '@/api/erp/master'
import { listAddressRegions, parseAddress, searchAddressRegions, type AddressParseResult, type AddressRegionOption } from '@/api/erp/address'
import { uploadFile } from '@/api/sys/file'
import { addStaff, updateStaffStatus, type StaffForm } from '@/api/system/staff'
import { allocatedUserList, getActiveRoles, type Role } from '@/api/sys/role'
import { useUserStore } from '@/store/modules/user'

type EmployeeOption = { userId: string, username?: string, nickname?: string, status?: number }
type SaleDraftRecord = { savedAt?: number, form?: ErpBill }
type DraftSaveStatus = 'idle' | 'saving' | 'saved' | 'error'
type AddressCascaderOption = AddressRegionOption & { children?: AddressCascaderOption[] }
type AddressRegionPickerTarget = 'sale' | 'quickCustomer'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const saleDraftPrefix = 'lingnow-erp:sale-draft'
const ADDRESS_REGION_MAX_DEPTH = 5
const draftReady = ref(false)
const restoringDraft = ref(false)
const skipLeavePrompt = ref(false)
const saleDraftMeta = ref<SaleDraftRecord>()
const draftSaveStatus = ref<DraftSaveStatus>('idle')
let draftSaveTimer: ReturnType<typeof setTimeout> | undefined
let draftStatusTimer: ReturnType<typeof setTimeout> | undefined
const module = computed<BillModule>(() => {
  if (route.path.includes('/production')) return 'production'
  if (route.path.includes('/sale-return')) return 'sale-return'
  if (route.path.includes('/purchase-return')) return 'purchase-return'
  return route.path.includes('/purchase') ? 'purchase' : 'sale'
})
const titleMap: Record<BillModule, string> = { sale: '新增销售单', 'sale-return': '新增销售退货', purchase: '新增进货单', 'purchase-return': '新增进货退货', production: '生产单' }
const title = computed(() => titleMap[module.value])
const isProduction = computed(() => module.value === 'production')
const isSaleLike = computed(() => module.value.startsWith('sale') || isProduction.value)
const partnerLabel = computed(() => isSaleLike.value ? '客户' : '供应商')
const billRemarkLabel = computed(() => isSaleLike.value ? '订单留言' : '备注')
const showReceiver = computed(() => isSaleLike.value)
const showEmployeeField = computed(() => isSaleLike.value)
const showLogoUpload = computed(() => module.value === 'sale' || isProduction.value)
const saleDraftEnabled = computed(() => module.value === 'sale' && !route.query.id && !form.value.id)
const saleDraftAvailable = computed(() => Boolean(saleDraftEnabled.value && saleDraftMeta.value?.form && hasSaleDraftContent(saleDraftMeta.value.form)))
const saleDraftSavedAtText = computed(() => saleDraftMeta.value?.savedAt ? `保存于 ${formatDraftTime(saleDraftMeta.value.savedAt)}` : '已保存')
const saleDraftSummary = computed(() => {
  const draft = saleDraftMeta.value?.form
  const items = draft?.items || []
  const partner = partners.value.find(item => String(item.id) === String(draft?.partnerId || ''))
  return {
    partner: partner?.name || draft?.receiverName || '-',
    receiver: draft?.receiverName || '-',
    itemCount: items.filter(item => item.productId || item.productName || item.productCode || item.remark || item.logoImageUrl).length,
    logoCount: items.filter(item => item.logoImageUrl).length
  }
})
const draftSaveStatusText = computed(() => {
  if (!saleDraftEnabled.value) return ''
  if (draftSaveStatus.value === 'saving') return '草稿保存中...'
  if (draftSaveStatus.value === 'saved') return saleDraftMeta.value?.savedAt ? `草稿已保存 ${formatDraftTime(saleDraftMeta.value.savedAt)}` : '草稿已保存'
  if (draftSaveStatus.value === 'error') return '草稿保存失败'
  if (saleDraftAvailable.value) return saleDraftSavedAtText.value
  return ''
})
const formRef = ref()
const products = ref<ErpProduct[]>([])
const categories = ref<ErpMasterVO[]>([])
const attributes = ref<ErpMasterVO[]>([])
const partners = ref<ErpMasterVO[]>([])
const warehouses = ref<ErpMasterVO[]>([])
const accounts = ref<ErpMasterVO[]>([])
const employeeUsers = ref<EmployeeOption[]>([])
const activeRoles = ref<Role[]>([])
const addressOpen = ref(false)
const addressRawText = ref('')
const addressResult = ref<AddressParseResult>()
const addressEditForm = reactive<AddressParseResult>({})
const addressLoading = ref(false)
const addressRegionPath = ref<string[]>([])
const addressRegionPickerOpen = ref(false)
const addressRegionSearchKeyword = ref('')
const addressRegionSearchLoading = ref(false)
const addressRegionSearchResults = ref<AddressRegionOption[]>([])
const addressAreaOptions = ref<AddressCascaderOption[]>([])
const addressAreaLoading = ref(false)
const addressChildrenCache = new Map<string, AddressCascaderOption[]>()
const addressChildrenRequests = new Map<string, Promise<AddressCascaderOption[]>>()
const addressRegionSearchTimers: Partial<Record<AddressRegionPickerTarget, ReturnType<typeof setTimeout>>> = {}
const addressRegionSearchSeq: Record<AddressRegionPickerTarget, number> = { sale: 0, quickCustomer: 0 }
const quickCustomerOpen = ref(false)
const quickCustomerSaving = ref(false)
const quickCustomerFormRef = ref()
const quickCustomerAddressOpen = ref(false)
const quickCustomerAddressRawText = ref('')
const quickCustomerAddressResult = ref<AddressParseResult>()
const quickCustomerAddressEditForm = reactive<AddressParseResult>({})
const quickCustomerAddressLoading = ref(false)
const quickCustomerAddressRegionPath = ref<string[]>([])
const quickCustomerAddressRegionPickerOpen = ref(false)
const quickCustomerAddressRegionSearchKeyword = ref('')
const quickCustomerAddressRegionSearchLoading = ref(false)
const quickCustomerAddressRegionSearchResults = ref<AddressRegionOption[]>([])
const quickWarehouseOpen = ref(false)
const quickWarehouseSaving = ref(false)
const quickWarehouseFormRef = ref()
const quickEmployeeOpen = ref(false)
const quickEmployeeSaving = ref(false)
const quickEmployeeFormRef = ref()
const employeeDisableLoadingId = ref('')
const quickCustomerForm = reactive<ErpMasterForm>({
  code: '',
  name: '',
  parentId: '0',
  contact: '',
  phone: '',
  address: '',
  sortOrder: 0,
  status: 1,
  remark: ''
})
const quickCustomerRules = {
  code: [{ required: true, message: '客户编码不能为空', trigger: 'blur' }]
}
const quickWarehouseForm = reactive<ErpMasterForm>({
  code: '',
  name: '',
  parentId: '0',
  sortOrder: 0,
  status: 1,
  remark: ''
})
const quickWarehouseRules = {
  name: [{ required: true, message: '仓库名称不能为空', trigger: 'blur' }]
}
const quickEmployeeForm = reactive<StaffForm>({
  username: '',
  nickname: '',
  password: '',
  status: 1,
  roleIds: []
})
const quickEmployeeRules = {
  nickname: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
  username: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
}
const paymentMethods = ['淘宝', '1688', '小红书', '微信', '支付宝']
const readonly = computed(() => isProduction.value)
const hideSaleFinancialFields = computed(() => false)
const canEditLineAmount = computed(() => !hideSaleFinancialFields.value)
const canViewFinancialSummary = computed(() => !hideSaleFinancialFields.value)
const paymentRequired = computed(() => Number(form.value.paidAmount || 0) > 0)
const state = reactive({
  form: { billDate: new Date().toISOString().slice(0, 10), partnerId: '', warehouseId: '', paymentMethod: '', paidAmount: 0, discountAmount: 0, otherAmount: 0, items: [] } as ErpBill,
  rules: {
    billDate: [{ required: true, message: '日期不能为空', trigger: 'change' }],
    partnerId: [{ required: true, message: '往来单位不能为空', trigger: 'change' }],
    warehouseId: [{ required: true, message: '仓库不能为空', trigger: 'change' }],
    accountId: [{
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (paymentRequired.value && !value) {
          callback(new Error('请选择账户'))
          return
        }
        callback()
      },
      trigger: 'change'
    }],
    paymentMethod: [{
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (paymentRequired.value && !value) {
          callback(new Error('请选择付款方式'))
          return
        }
        callback()
      },
      trigger: 'change'
    }]
  }
})
const { form, rules } = toRefs(state)
const isAdminUser = computed(() => {
  const username = (userStore.userInfo?.username || '').toLowerCase()
  const roles = ((userStore.userInfo as any)?.roles || []) as Array<{ roleKey?: string, roleName?: string }>
  return hasPermission('*:*:*') || username === 'admin' || username === 'superadmin' || roles.some(role => role.roleKey === 'admin')
})
const productionEditable = computed(() => isProduction.value && isAdminUser.value)
const savePermission = computed(() => form.value.id ? `erp:${module.value}:edit` : `erp:${module.value}:add`)
const showSaveButton = computed(() => isProduction.value ? !!form.value.id && productionEditable.value : true)
const canManageEmployee = computed(() => showEmployeeField.value && !readonly.value && isAdminUser.value)
const employeeSelectValue = computed({
  get: () => {
    const currentId = form.value.employeeId ? String(form.value.employeeId) : ''
    return employeeSelectOptions.value.some(item => String(item.userId) === currentId) ? currentId : ''
  },
  set: (value: string) => employeeChanged(value)
})
const employeeSelectOptions = computed<EmployeeOption[]>(() => {
  const options = employeeUsers.value.filter(item => !isHiddenEmployee(item))
  const currentId = form.value.employeeId ? String(form.value.employeeId) : ''
  if (currentId && !options.some(item => String(item.userId) === currentId)) {
    const current = { userId: currentId, username: form.value.employeeName || currentId, nickname: form.value.employeeName || currentId }
    if (!isHiddenEmployee(current)) {
      options.unshift(current)
    }
  }
  return options
})
const itemProductRules = [{ required: true, message: '请选择商品', trigger: 'change' }]
const itemQtyRules = [{
  validator: (_rule: unknown, value: number, callback: (error?: Error) => void) => {
    if (Number(value || 0) <= 0) {
      callback(new Error('数量必须大于0'))
      return
    }
    callback()
  },
  trigger: 'change'
}]
const totalQty = computed(() => form.value.items.reduce((sum, row) => sum + Number(row.qty || 0), 0).toFixed(2))
const totalAmount = computed(() => form.value.items.reduce((sum, row) => sum + Number(row.amount || 0), 0).toFixed(2))
const costAmount = computed(() => form.value.items.reduce((sum, row) => sum + Number(row.qty || 0) * Number(row.purchasePrice || 0), 0).toFixed(2))
const payableAmount = computed(() => (Number(totalAmount.value) - Number(form.value.discountAmount || 0) + Number(form.value.otherAmount || 0)).toFixed(2))
const profitAmount = computed(() => (Number(form.value.paidAmount || 0) - Number(costAmount.value)).toFixed(2))
const categoryTree = computed(() => buildTree(categories.value))
const attributeGroups = computed(() => uniqueById(attributes.value.filter(item => String(item.parentId || '0') === '0' && item.status === 1)))
const showCostPrice = computed(() => isSaleLike.value)
const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '')
let productOptionsRequestSeq = 0

function loadOptions() {
  return Promise.all([
    productOptions().then(res => products.value = res),
    listMaster('product-category', { current: 1, size: 1000 }).then(res => categories.value = res.records),
    listMaster('product-attribute', { current: 1, size: 1000 }).then(res => attributes.value = res.records),
	    loadPartners(),
	    loadWarehouses(),
	    listMaster('account', { current: 1, size: 200 }).then(res => accounts.value = res.records),
	    canManageEmployee.value ? loadEmployeeUsers() : Promise.resolve()
	  ])
}
async function ensureSalespersonRole() {
  if (!activeRoles.value.length) {
    activeRoles.value = await getActiveRoles() as Role[]
  }
  const role = activeRoles.value.find(item => item.roleKey === 'salesperson')
  if (!role) {
    throw new Error('未找到业务员角色')
  }
  return role
}
async function loadEmployeeUsers() {
  const role = await ensureSalespersonRole()
  const res: any = await allocatedUserList({ current: 1, size: 1000, roleId: role.roleId })
  employeeUsers.value = (res.records || []).map((item: any) => ({
    userId: String(item.userId),
    username: item.username,
    nickname: item.nickname,
    status: item.status
  })).filter(item => (item.status === undefined || Number(item.status) === 1) && !isHiddenEmployee(item))
  return employeeUsers.value
}
function loadPartners() {
  return listMaster(isSaleLike.value ? 'customer' : 'supplier', { current: 1, size: 200 }).then(res => {
    partners.value = res.records
    return res.records
  })
}
function loadWarehouses() {
  return listMaster('warehouse', { current: 1, size: 200 }).then(res => {
    warehouses.value = res.records
    return res.records
  })
}
async function loadData() {
  const id = route.query.id as string
  if (id) {
    getBill(module.value, id).then(res => {
      form.value = { ...res, employeeId: res.employeeId ? String(res.employeeId) : '', productionUserId: res.productionUserId ? String(res.productionUserId) : '', productionUserName: res.productionUserName || '', items: (res.items || []).map(prepareBillItem) }
      form.value.items.forEach(row => {
        loadRowProducts(row)
        hydrateRowProductSnapshot(row)
      })
      draftReady.value = true
    })
  } else {
    if (isProduction.value) {
      ElMessage.warning('生产单只能查看已有销售单')
      back()
      return
    }
    fillCurrentEmployee()
    const no = await nextBillNo(module.value)
    form.value.billNo = no
    loadSaleDraftMeta()
    draftReady.value = true
  }
}
function saleDraftStorageKey() {
  const user = userStore.userInfo as any
  return `${saleDraftPrefix}:${user?.userId || user?.username || 'anonymous'}`
}
function saleDraftFormSnapshot(source: ErpBill = form.value) {
  const snapshot = JSON.parse(JSON.stringify(source || {})) as ErpBill
  snapshot.id = undefined
  snapshot.billType = undefined
  snapshot.items = (snapshot.items || []).map(({ optionProducts, productOptionsRequestId, ...item }) => item)
  return snapshot
}
function hasSaleDraftContent(source: ErpBill = form.value) {
  const items = source.items || []
  return Boolean(
    source.partnerId ||
    source.warehouseId ||
    source.accountId ||
    source.receiverName ||
    source.receiverPhone ||
    source.receiverAddress ||
    source.paymentMethod ||
    source.remark ||
    Number(source.paidAmount || 0) > 0 ||
    Number(source.discountAmount || 0) > 0 ||
    Number(source.otherAmount || 0) > 0 ||
    items.some(row => Boolean(
      row.productId ||
      row.productCode ||
      row.logoImageUrl ||
      row.remark ||
      row.optionAttributeIds ||
      row.optionAttributeText ||
      Number(row.qty || 0) !== 1 ||
      Number(row.price || 0) > 0
    ))
  )
}
function clearSaleDraft() {
  if (draftSaveTimer) {
    clearTimeout(draftSaveTimer)
    draftSaveTimer = undefined
  }
  try {
    localStorage.removeItem(saleDraftStorageKey())
  } catch {
    // localStorage can be blocked by browser settings.
  }
  saleDraftMeta.value = undefined
  draftSaveStatus.value = 'idle'
}
function saveSaleDraftNow() {
  if (!saleDraftEnabled.value || restoringDraft.value) return
  if (!hasSaleDraftContent()) {
    clearSaleDraft()
    return
  }
  const savedAt = Date.now()
  try {
    const draft = {
      savedAt,
      form: saleDraftFormSnapshot()
    }
    localStorage.setItem(saleDraftStorageKey(), JSON.stringify(draft))
    saleDraftMeta.value = draft
    markDraftSaveStatus('saved')
  } catch {
    markDraftSaveStatus('error')
    // Ignore quota and privacy-mode errors; the normal save button still works.
  }
}
function queueSaleDraftSave() {
  if (!draftReady.value || restoringDraft.value) return
  if (!saleDraftEnabled.value) return
  if (!hasSaleDraftContent()) {
    clearSaleDraft()
    return
  }
  draftSaveStatus.value = 'saving'
  if (draftSaveTimer) clearTimeout(draftSaveTimer)
  draftSaveTimer = setTimeout(() => {
    draftSaveTimer = undefined
    saveSaleDraftNow()
  }, 300)
}
function loadSaleDraftMeta() {
  if (!saleDraftEnabled.value) {
    saleDraftMeta.value = undefined
    return undefined
  }
  let raw = ''
  try {
    raw = localStorage.getItem(saleDraftStorageKey()) || ''
  } catch {
    saleDraftMeta.value = undefined
    return undefined
  }
  if (!raw) {
    saleDraftMeta.value = undefined
    return undefined
  }
  let draft: { savedAt?: number, form?: ErpBill }
  try {
    draft = JSON.parse(raw)
  } catch {
    clearSaleDraft()
    return undefined
  }
  if (!draft.form || !hasSaleDraftContent(draft.form)) {
    clearSaleDraft()
    return undefined
  }
  saleDraftMeta.value = draft
  return draft
}
function applySaleDraft(draft: SaleDraftRecord, nextNo?: string) {
  if (!draft.form) return
  restoringDraft.value = true
  try {
    form.value = {
      ...draft.form,
      id: undefined,
      billType: undefined,
      billNo: nextNo || form.value.billNo || draft.form.billNo || '',
      billDate: draft.form.billDate || new Date().toISOString().slice(0, 10),
      items: (draft.form.items || []).map(item => prepareBillItem({ ...item }))
    }
    fillCurrentEmployee()
    form.value.items.forEach(row => {
      loadRowProducts(row)
      hydrateRowProductSnapshot(row)
    })
  } finally {
    restoringDraft.value = false
  }
}
async function restoreSaleDraftFromBox() {
  const draft = loadSaleDraftMeta()
  if (!draft?.form) {
    ElMessage.info('暂无可恢复草稿')
    return
  }
  if (hasSaleDraftContent()) {
    try {
      await ElMessageBox.confirm('恢复草稿会覆盖当前未保存内容，确定恢复吗？', '恢复草稿', {
        confirmButtonText: '恢复',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
  }
  applySaleDraft(draft)
  ElMessage.success('已恢复未保存草稿')
}
async function clearSaleDraftFromBox() {
  if (!saleDraftAvailable.value) return
  try {
    await ElMessageBox.confirm('确定清空当前销售单草稿吗？', '清空草稿', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  clearSaleDraft()
  ElMessage.success('草稿已清空')
}
function markDraftSaveStatus(status: DraftSaveStatus) {
  draftSaveStatus.value = status
  if (draftStatusTimer) {
    clearTimeout(draftStatusTimer)
    draftStatusTimer = undefined
  }
  if (status === 'error') return
  if (status === 'saved') {
    draftStatusTimer = setTimeout(() => {
      draftStatusTimer = undefined
      if (draftSaveStatus.value === 'saved') {
        draftSaveStatus.value = 'idle'
      }
    }, 3000)
  }
}
function formatDraftTime(value: number) {
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}
function hasUnsavedSaleDraft() {
  return saleDraftEnabled.value && hasSaleDraftContent()
}
function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (!hasUnsavedSaleDraft()) return
  saveSaleDraftNow()
  event.preventDefault()
  event.returnValue = ''
}
function fillCurrentEmployee() {
  if (!showEmployeeField.value) return
  hydrateCurrentEmployeeId()
  if (!canManageEmployee.value && !form.value.employeeName) {
    form.value.employeeName = currentUserName.value
  }
}
function hydrateCurrentEmployeeId() {
  if (!canManageEmployee.value || form.value.employeeId || !userStore.userInfo?.username) return
  const current = employeeUsers.value.find(item => item.username === userStore.userInfo?.username)
  if (!current) return
  form.value.employeeId = current.userId
  form.value.employeeName = employeeLabel(current)
}
function addRow() {
  const row = prepareBillItem({ productId: '', qty: 1, price: 0 } as BillItem)
  form.value.items.push(row)
  loadRowProducts(row)
}
function prepareBillItem(row: BillItem) {
  row.attributeSelections = attributeSelectionsFromIds(row.optionAttributeIds)
  row.availableAttributeIds = row.availableAttributeIds || attributeGroupIdsFromOptionIds(row.optionAttributeIds)
  if (!row.categoryLevel1Id && row.categoryLevel2Id) {
    const parent = parentCategory(row.categoryLevel2Id)
    row.categoryLevel1Id = parent?.id
    row.categoryLevel1Name = parent?.name
  }
  return row
}
function rowProductOptions(row: BillItem) {
  return Array.isArray(row.optionProducts) ? row.optionProducts : products.value
}
function loadRowProducts(row: BillItem, keyword = '') {
  const requestId = ++productOptionsRequestSeq
  row.productOptionsRequestId = requestId
  const params = {
    ...(keyword ? { keyword } : {})
  }
  productOptions(params).then(async res => {
    if (row.productOptionsRequestId !== requestId) return
    if (!keyword && row.productId && !res.some(item => String(item.id) === String(row.productId)) && row.productCode) {
      const selectedOptions = await productOptions({ keyword: row.productCode }).catch(() => [])
      if (row.productOptionsRequestId !== requestId) return
      res = uniqueProductsById([...selectedOptions, ...res])
    }
    const current = rowProduct(row)
    const selected = rowSelectedProduct(row)
    const options = res.map(item => String(item.id) === String(row.productId || '') ? mergeProductOption(current, item) : item)
    row.optionProducts = selected && !options.some(item => String(item.id) === String(selected.id))
      ? [selected, ...options]
      : options
    hydrateRowProductAttributesFromOptions(row)
  }).catch(() => {
    if (row.productOptionsRequestId !== requestId) return
    const selected = rowSelectedProduct(row)
    row.optionProducts = selected ? [selected] : []
  })
}
function clearRowProduct(row: BillItem) {
  row.productId = ''
  row.productCode = undefined
  row.productName = undefined
  row.productImageUrl = undefined
  row.spec = undefined
  row.unitId = undefined
  row.unitName = undefined
  row.purchasePrice = 0
  row.basePrice = undefined
  row.price = 0
  row.amount = 0
  row.attributeSelections = {}
  row.availableAttributeIds = ''
  row.availableAttributeText = ''
  row.productOptionsRequestId = undefined
  row.optionAttributeIds = ''
  row.optionAttributeText = ''
  row.attributeText = ''
  calc()
}
async function productChanged(row: BillItem) {
  const selected = rowProductOptions(row).find(item => String(item.id) === String(row.productId)) || products.value.find(item => String(item.id) === String(row.productId))
  const productId = row.productId ? String(row.productId) : ''
  if (!selected && !productId) return
  let product: ErpProduct | undefined = selected
  if (productId) {
    product = await getProduct(productId).catch(() => selected)
    if (String(row.productId || '') !== productId) return
    if (!product) return
    upsertProductOption(row, product)
  }
  if (!product) return
  rememberRowProductAttributes(row, product)
  row.productCode = product.code
  row.productName = product.name
  row.productImageUrl = product.imageUrl
  row.spec = product.spec
  row.unitId = product.unitId
  row.purchasePrice = Number(product.purchasePrice || 0)
  row.basePrice = isSaleLike.value ? Number(product.salePrice || 0) : Number(product.purchasePrice || 0)
  row.price = row.basePrice
  applyProductDefaultCategory(row, product)
  row.attributeSelections = keepAllowedSelections(row, {})
  syncRowSnapshot(row)
  applyAttributeExtraPrice(row)
  calc()
}
function removeRow(index: number) {
  form.value.items.splice(index, 1)
  calc()
}
function buildTree(records: ErpMasterVO[]) {
  const map = new Map<string, ErpMasterVO & { children: ErpMasterVO[] }>()
  records.forEach(item => map.set(item.id, { ...item, children: [] }))
  const roots: (ErpMasterVO & { children: ErpMasterVO[] })[] = []
  map.forEach(item => {
    const parentId = String(item.parentId || '0')
    const parent = map.get(parentId)
    if (parent && parent.id !== item.id) {
      parent.children.push(item)
    } else {
      roots.push(item)
    }
  })
  const sort = (items: (ErpMasterVO & { children: ErpMasterVO[] })[]) => {
    items.sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
    items.forEach(item => sort(item.children as (ErpMasterVO & { children: ErpMasterVO[] })[]))
  }
  sort(roots)
  return roots
}
function parentCategory(categoryId: string) {
  const current = categories.value.find(item => item.id === categoryId)
  if (!current) return undefined
  return categories.value.find(item => item.id === String(current.parentId || '0'))
}
function categoryPath(categoryId?: string) {
  const byId = new Map(categories.value.map(item => [item.id, item]))
  const path: ErpMasterVO[] = []
  let current = categoryId ? byId.get(categoryId) : undefined
  while (current) {
    path.unshift(current)
    const parentId = String(current.parentId || '0')
    current = byId.get(parentId)
  }
  return path[0]?.name === '商品' ? path.slice(1) : path
}
function applyProductDefaultCategory(row: BillItem, product: ErpProduct) {
  if (!product.categoryId) return
  const path = categoryPath(product.categoryId)
  if (!path.length) return
  row.categoryLevel1Id = path[0]?.id
  row.categoryLevel1Name = path[0]?.name
  row.categoryLevel2Id = path[1]?.id
  row.categoryLevel2Name = path[1]?.name
}
function syncRowSnapshot(row: BillItem) {
  row.categoryLevel1Name = categories.value.find(item => String(item.id) === String(row.categoryLevel1Id || ''))?.name || row.categoryLevel1Name
  row.categoryLevel2Name = categories.value.find(item => String(item.id) === String(row.categoryLevel2Id || ''))?.name || row.categoryLevel2Name
  row.attributeSelections = keepAllowedSelections(row, row.attributeSelections || {})
  const selectedIds = Object.values(row.attributeSelections).filter(Boolean)
  const byId = new Map(attributes.value.map(item => [String(item.id), item]))
  row.optionAttributeIds = selectedIds.join(',')
  row.optionAttributeText = selectedIds
    .map(id => {
      const option = byId.get(String(id))
      const group = option ? byId.get(String(option.parentId || '0')) : undefined
      return option ? `${group?.name || '商品属性'}: ${option.name}` : ''
    })
    .filter(Boolean)
    .join(' / ')
  row.attributeText = rowLinePath(row)
}
function rowLinePath(row: BillItem) {
  return [row.optionAttributeText, row.remark].filter(Boolean).join(' / ')
}
function lineAmountText(row: BillItem) {
  return canViewFinancialSummary.value ? Number(row.amount || 0).toFixed(2) : '-'
}
function costPriceText(row: BillItem) {
  return row.purchasePrice === undefined || row.purchasePrice === null ? '-' : Number(row.purchasePrice || 0).toFixed(2)
}
function employeeLabel(user: EmployeeOption) {
  return user.nickname || user.username || String(user.userId)
}
function isHiddenEmployee(user: EmployeeOption) {
  const username = (user.username || '').trim().toLowerCase()
  const label = employeeLabel(user).trim()
  return username === 'admin' || username === 'superadmin' || label === '超级管理员' || label.includes('测试业务员')
}
function employeeChanged(value: string) {
  if (!value) {
    form.value.employeeId = undefined
    form.value.employeeName = ''
    return
  }
  const selected = employeeSelectOptions.value.find(item => String(item.userId) === String(value))
  form.value.employeeId = value
  form.value.employeeName = selected ? employeeLabel(selected) : ''
}
async function disableEmployee(user: EmployeeOption) {
  if (!canManageEmployee.value || !user.userId) return
  const userId = String(user.userId)
  const label = employeeLabel(user)
  try {
    await ElMessageBox.confirm(`确定禁用业务员“${label}”吗？禁用后不会物理删除。`, '禁用业务员', {
      confirmButtonText: '禁用',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  employeeDisableLoadingId.value = userId
  try {
    await updateStaffStatus(userId, 0)
    if (String(form.value.employeeId || '') === userId) {
      form.value.employeeId = undefined
      form.value.employeeName = ''
    }
    employeeUsers.value = employeeUsers.value.filter(item => String(item.userId) !== userId)
    await loadEmployeeUsers()
    ElMessage.success('业务员已禁用')
  } finally {
    if (employeeDisableLoadingId.value === userId) {
      employeeDisableLoadingId.value = ''
    }
  }
}
function hasPermission(permission: string) {
  const permissions = JSON.parse(localStorage.getItem('permissions') || '[]') as string[]
  return permissions.includes('*:*:*') || permissions.includes(permission)
}
function financialSummaryText(value: string) {
  return canViewFinancialSummary.value ? value : '-'
}
function splitIds(value?: string) {
  return uniqueIds(value ? value.split(',').map(item => item.trim()).filter(Boolean) : [])
}
function uniqueIds(values: Array<string | number | undefined>) {
  return Array.from(new Set(values.map(item => String(item || '').trim()).filter(Boolean)))
}
function uniqueById(records: ErpMasterVO[]) {
  const seen = new Set<string>()
  return records.filter(item => {
    const id = String(item.id)
    if (seen.has(id)) return false
    seen.add(id)
    return true
  })
}
function attributeSelectionsFromIds(value?: string) {
  const selections: Record<string, string> = {}
  const byId = new Map(attributes.value.map(item => [String(item.id), item]))
  splitIds(value).forEach(id => {
    const option = byId.get(String(id))
    const groupId = String(option?.parentId || '')
    if (option && groupId && groupId !== '0') {
      selections[groupId] = String(option.id)
    }
  })
  return selections
}
function attributeGroupIdsFromOptionIds(value?: string) {
  const groupIds: string[] = []
  splitIds(value).forEach(id => {
    const option = attributes.value.find(item => String(item.id) === String(id))
    const groupId = String(option?.parentId || '')
    if (groupId && groupId !== '0') {
      groupIds.push(groupId)
    }
  })
  return uniqueIds(groupIds).join(',')
}
function rowProduct(row: BillItem) {
  return rowProductOptions(row).find(item => String(item.id) === String(row.productId)) || products.value.find(item => String(item.id) === String(row.productId))
}
function rowSelectedProduct(row: BillItem) {
  if (!row.productId || (!row.productCode && !row.productName)) return undefined
  const current = rowProduct(row)
  return {
    ...current,
    id: row.productId,
    code: row.productCode || current?.code || '',
    name: row.productName || current?.name || '',
    spec: row.spec || current?.spec,
    imageUrl: row.productImageUrl || current?.imageUrl,
    unitId: row.unitId || current?.unitId,
    unitName: row.unitName || current?.unitName,
    purchasePrice: row.purchasePrice ?? current?.purchasePrice,
    salePrice: row.basePrice ?? current?.salePrice,
    attributeIds: row.availableAttributeIds || current?.attributeIds,
    attributeText: row.availableAttributeText || row.attributeText || current?.attributeText
  } as ErpProduct
}
function hydrateRowProductSnapshot(row: BillItem) {
  if (!row.productId) return
  getProduct(String(row.productId)).then(product => {
    upsertProductOption(row, product)
    hydrateRowProductAttributes(row, product)
    row.productCode = product.code || row.productCode
    row.productName = product.name || row.productName
    row.productImageUrl = product.imageUrl || row.productImageUrl
    row.spec = product.spec || row.spec
    row.unitId = product.unitId || row.unitId
    row.unitName = product.unitName || row.unitName
    if (showCostPrice.value) {
      row.purchasePrice = Number(product.purchasePrice || 0)
    }
    if (row.basePrice === undefined || row.basePrice === null) {
      row.basePrice = isSaleLike.value ? Number(product.salePrice || 0) : Number(product.purchasePrice || 0)
    }
  }).catch(() => {
    hydrateRowProductAttributesFromOptions(row)
    row.attributeSelections = keepAllowedSelections(row, row.attributeSelections || {})
  })
}
function rowAttributeGroups(row: BillItem) {
  const product = rowProduct(row)
  const groupIds = splitIds(row.availableAttributeIds || product?.attributeIds)
  if (!groupIds.length) {
    splitIds(row.optionAttributeIds).forEach(id => {
      const option = attributes.value.find(item => String(item.id) === String(id))
      const groupId = String(option?.parentId || '')
      if (groupId && groupId !== '0') {
        groupIds.push(groupId)
      }
    })
  }
  if (!groupIds.length) return []
  return attributeGroups.value.filter(item => groupIds.includes(String(item.id)))
}
function rememberRowProductAttributes(row: BillItem, product: ErpProduct) {
  row.availableAttributeIds = product.attributeIds || ''
  row.availableAttributeText = product.attributeText || ''
}
function hydrateRowProductAttributes(row: BillItem, product?: ErpProduct) {
  if (!product) return
  rememberRowProductAttributes(row, product)
  row.attributeSelections = keepAllowedSelections(row, row.attributeSelections || {})
}
function hydrateRowProductAttributesFromOptions(row: BillItem) {
  hydrateRowProductAttributes(row, rowProduct(row))
}
function attributeOptions(groupId: string) {
  return uniqueById(attributes.value
    .filter(item => String(item.parentId || '0') === groupId && item.status === 1)
    .sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0)))
}
function attributeOptionLabel(item: ErpMasterVO) {
  const extraAmount = Number(item.extraAmount || 0)
  return extraAmount > 0 ? `${item.name}（+${extraAmount.toFixed(2)}）` : item.name
}
function keepAllowedSelections(row: BillItem, selections: Record<string, string>) {
  const allowedGroupIds = new Set(rowAttributeGroups(row).map(item => String(item.id)))
  const next: Record<string, string> = {}
  Object.entries(selections).forEach(([groupId, optionId]) => {
    const normalizedGroupId = String(groupId)
    const normalizedOptionId = String(optionId)
    if (allowedGroupIds.has(normalizedGroupId) && attributeOptions(normalizedGroupId).some(item => String(item.id) === normalizedOptionId)) {
      next[normalizedGroupId] = normalizedOptionId
    }
  })
  return next
}
function upsertProductOption(row: BillItem, product: ErpProduct) {
  const optionProducts = rowProductOptions(row)
  const exists = optionProducts.some(item => String(item.id) === String(product.id))
  row.optionProducts = exists
    ? optionProducts.map(item => String(item.id) === String(product.id) ? mergeProductOption(item, product) : item)
    : [product, ...optionProducts]
  products.value = products.value.some(item => String(item.id) === String(product.id))
    ? products.value.map(item => String(item.id) === String(product.id) ? mergeProductOption(item, product) : item)
    : [product, ...products.value]
}
function uniqueProductsById(records: ErpProduct[]) {
  const seen = new Set<string>()
  return records.filter(item => {
    const id = String(item.id || '')
    if (!id || seen.has(id)) return false
    seen.add(id)
    return true
  })
}
function mergeProductOption(current: ErpProduct | undefined, next: ErpProduct) {
  if (!current) return next
  const merged = { ...current, ...next }
  if (!merged.attributeIds && current.attributeIds) {
    merged.attributeIds = current.attributeIds
  }
  if (!merged.attributeText && current.attributeText) {
    merged.attributeText = current.attributeText
  }
  return merged
}
function attributeSelectionChanged(row: BillItem) {
  syncRowSnapshot(row)
  applyAttributeExtraPrice(row)
}
function attributeExtraAmount(row: BillItem) {
  const selectedIds = Object.values(row.attributeSelections || {}).filter(Boolean).map(String)
  if (!selectedIds.length) return 0
  const byId = new Map(attributes.value.map(item => [String(item.id), item]))
  return selectedIds.reduce((sum, id) => sum + Number(byId.get(id)?.extraAmount || 0), 0)
}
function baseLinePrice(row: BillItem) {
  if (row.basePrice !== undefined && row.basePrice !== null) {
    return Number(row.basePrice || 0)
  }
  const product = rowProduct(row)
  if (product) {
    return isSaleLike.value ? Number(product.salePrice || 0) : Number(product.purchasePrice || 0)
  }
  return Math.max(0, Number(row.price || 0) - attributeExtraAmount(row))
}
function applyAttributeExtraPrice(row: BillItem) {
  if (!isSaleLike.value) return
  row.basePrice = baseLinePrice(row)
  row.price = Number((Number(row.basePrice || 0) + attributeExtraAmount(row)).toFixed(2))
  calc()
}
async function uploadLogoImage(row: BillItem, options: any) {
  const file = options.file as File
  if (!file.type?.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    options.onError?.(new Error('请选择图片文件'))
    return
  }
  const data = new FormData()
  data.append('file', file)
  try {
    const url = await uploadFile(data) as string
    row.logoImageUrl = url
    options.onSuccess?.(url)
    saveSaleDraftNow()
    ElMessage.success('LOGO图片上传成功')
  } catch (error) {
    options.onError?.(error)
  }
}
function clearLogoImage(row: BillItem) {
  row.logoImageUrl = ''
  saveSaleDraftNow()
}
function calc() {
  form.value.items.forEach(row => {
    row.amount = Number(row.qty || 0) * Number(row.price || 0)
  })
}
function openAddressParse() {
  addressRawText.value = [form.value.receiverName, form.value.receiverPhone, form.value.receiverAddress].filter(Boolean).join(' ')
  addressResult.value = undefined
  addressRegionPath.value = []
  fillAddressEditForm(addressEditForm, {
    contactName: form.value.receiverName || '',
    phone: form.value.receiverPhone || '',
    detailAddress: form.value.receiverAddress || ''
  })
  resolveAddressRegionPath(addressEditForm, addressRegionPath)
  addressOpen.value = true
  loadAddressRootOptions()
}
function doParseAddress() {
  if (!addressRawText.value.trim()) {
    ElMessage.warning('请先粘贴需要识别的地址内容')
    return
  }
  addressLoading.value = true
  parseAddress(addressRawText.value).then(res => {
    addressResult.value = res
    fillAddressEditForm(addressEditForm, res)
    resolveAddressRegionPath(addressEditForm, addressRegionPath, true)
  }).finally(() => addressLoading.value = false)
}
function applyAddress() {
  if (addressEditForm.contactName) form.value.receiverName = addressEditForm.contactName
  if (addressEditForm.phone) form.value.receiverPhone = addressEditForm.phone
  const addressText = addressFullText(addressEditForm)
  if (addressText) form.value.receiverAddress = addressText
  const matched = partners.value.find(item => item.phone && addressEditForm.phone && item.phone === addressEditForm.phone)
  if (matched) {
    form.value.partnerId = matched.id
    ElMessage.success(`已匹配客户：${matched.name}`)
  }
  addressOpen.value = false
}
function resetQuickCustomer() {
  quickCustomerForm.code = `CUS_${Date.now().toString().slice(-8)}`
  quickCustomerForm.name = ''
  quickCustomerForm.parentId = '0'
  quickCustomerForm.contact = form.value.receiverName || ''
  quickCustomerForm.phone = form.value.receiverPhone || ''
  quickCustomerForm.address = form.value.receiverAddress || ''
  quickCustomerForm.sortOrder = 0
  quickCustomerForm.status = 1
  quickCustomerForm.remark = ''
  quickCustomerAddressRawText.value = ''
  quickCustomerAddressResult.value = undefined
}
function openQuickCustomer() {
  resetQuickCustomer()
  quickCustomerOpen.value = true
}
function openQuickCustomerAddressParse() {
  quickCustomerAddressRawText.value = [quickCustomerForm.contact, quickCustomerForm.phone, quickCustomerForm.address].filter(Boolean).join(' ')
  quickCustomerAddressResult.value = undefined
  quickCustomerAddressRegionPath.value = []
  fillAddressEditForm(quickCustomerAddressEditForm, {
    contactName: quickCustomerForm.contact || '',
    phone: quickCustomerForm.phone || '',
    detailAddress: quickCustomerForm.address || ''
  })
  resolveAddressRegionPath(quickCustomerAddressEditForm, quickCustomerAddressRegionPath)
  quickCustomerAddressOpen.value = true
  loadAddressRootOptions()
}
function doParseQuickCustomerAddress() {
  if (!quickCustomerAddressRawText.value.trim()) {
    ElMessage.warning('请先粘贴需要识别的地址内容')
    return
  }
  quickCustomerAddressLoading.value = true
  parseAddress(quickCustomerAddressRawText.value).then(res => {
    quickCustomerAddressResult.value = res
    fillAddressEditForm(quickCustomerAddressEditForm, res)
    resolveAddressRegionPath(quickCustomerAddressEditForm, quickCustomerAddressRegionPath, true)
  }).finally(() => quickCustomerAddressLoading.value = false)
}
function applyQuickCustomerAddress() {
  if (quickCustomerAddressEditForm.contactName) quickCustomerForm.contact = quickCustomerAddressEditForm.contactName
  if (quickCustomerAddressEditForm.phone) quickCustomerForm.phone = quickCustomerAddressEditForm.phone
  const addressText = addressFullText(quickCustomerAddressEditForm)
  if (addressText) quickCustomerForm.address = addressText
  quickCustomerAddressOpen.value = false
}
function fillAddressEditForm(target: AddressParseResult, source?: AddressParseResult) {
  target.contactName = source?.contactName || ''
  target.phone = source?.phone || ''
  target.province = source?.province || ''
  target.city = source?.city || ''
  target.district = source?.district || ''
  target.street = source?.street || ''
  target.village = source?.village || ''
  target.regionPath = source?.regionPath || []
  target.regionPathNames = source?.regionPathNames || []
  target.contactCandidates = source?.contactCandidates || []
  target.detailAddress = source?.detailAddress || ''
}
function hasAddressEditValue(target?: AddressParseResult) {
  return Boolean(target && [target.contactName, target.phone, target.detailAddress].some(item => String(item || '').trim()))
}
async function loadAddressRootOptions() {
  if (addressAreaOptions.value.length) return
  addressAreaLoading.value = true
  try {
    addressAreaOptions.value = await loadAddressChildren('')
  } finally {
    addressAreaLoading.value = false
  }
}
async function loadAddressChildren(parentCode?: string) {
  const key = parentCode || '0'
  const cached = addressChildrenCache.get(key)
  if (cached) return cached
  const pending = addressChildrenRequests.get(key)
  if (pending) return pending
  const request = listAddressRegions(parentCode).then(list => list.map(item => ({
    code: item.code,
    name: item.name,
    level: item.level,
    leaf: item.leaf,
    path: item.path || [],
    pathNames: item.pathNames || []
  })))
  addressChildrenRequests.set(key, request)
  try {
    const children = await request
    addressChildrenCache.set(key, children)
    if (key === '0') {
      addressAreaOptions.value = children
    }
    return children
  } finally {
    addressChildrenRequests.delete(key)
  }
}
function addressRegionChanged(target: AddressParseResult, value: unknown, trimDetail = false) {
  const path = normalizeAddressPath(value)
  const labels = path.map(code => findAddressOptionByCode(addressAreaOptions.value, code)?.name || '').filter(Boolean)
  target.province = labels[0] || ''
  target.city = labels[1] || ''
  target.district = labels[2] || ''
  target.street = labels[3] || ''
  target.village = labels[4] || ''
  target.regionPath = path
  target.regionPathNames = labels
  if (trimDetail) {
    target.detailAddress = trimAddressDetailByRegionNames(target.detailAddress, labels)
  }
}
function normalizeAddressPath(value: unknown) {
  if (Array.isArray(value)) return value.map(item => String(item || '')).filter(Boolean)
  return value ? [String(value)] : []
}
async function resolveAddressRegionPath(target: AddressParseResult, pathRef: { value: string[] }, trimDetail = false) {
  try {
    const path = target.regionPath?.length ? target.regionPath : await findAddressRegionPath(target)
    await ensureAddressPathLoaded(path)
    pathRef.value = path
    addressRegionChanged(target, path, trimDetail)
  } catch (error) {
    pathRef.value = []
  }
}
async function findAddressRegionPath(target: AddressParseResult) {
  const provinceOptions = await loadAddressChildren('')
  const province = findAddressOptionByName(provinceOptions, target.province)
  if (!province) return []

  const path = [province.code]
  const cityOptions = await attachAddressChildren(province)
  const city = findAddressOptionByName(cityOptions, target.city) || findDirectCityOption(cityOptions, province.name)
  if (!city) return path

  path.push(city.code)
  const districtOptions = await attachAddressChildren(city)
  const district = findAddressOptionByName(districtOptions, target.district)
  if (!district) return path

  path.push(district.code)
  const streetOptions = await attachAddressChildren(district)
  const street = findAddressOptionByName(streetOptions, target.street) || findStreetOptionInText(streetOptions, target.detailAddress)
  if (street) path.push(street.code)
  if (street && target.village) {
    const villageOptions = await attachAddressChildren(street)
    const village = findAddressOptionByName(villageOptions, target.village) || findStreetOptionInText(villageOptions, target.detailAddress)
    if (village) path.push(village.code)
  }
  return path
}
async function ensureAddressPathLoaded(path: string[]) {
  if (!path.length) return
  let options = await loadAddressChildren('')
  for (const code of path) {
    const option = options.find(item => item.code === code)
    if (!option || option.leaf) return
    options = await attachAddressChildren(option)
  }
}
async function attachAddressChildren(option: AddressCascaderOption) {
  if (option.leaf) return []
  const children = await loadAddressChildren(option.code)
  option.children = children
  return children
}
async function refreshAddressRegionOptionsForSearch() {
  addressAreaOptions.value = [...addressAreaOptions.value]
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 0))
}
function normalizeAddressSearchText(value?: string) {
  return String(value || '').toLowerCase().replace(/[\s/／,，;；:：()（）[\]【】]+/g, '').trim()
}
function isAddressRegionLeafSelection(path: string[], current?: AddressCascaderOption) {
  return Boolean(current?.leaf || path.length >= ADDRESS_REGION_MAX_DEPTH)
}
function addressRegionPathRef(target: AddressRegionPickerTarget) {
  return target === 'sale' ? addressRegionPath : quickCustomerAddressRegionPath
}
function addressRegionPickerOpenRef(target: AddressRegionPickerTarget) {
  return target === 'sale' ? addressRegionPickerOpen : quickCustomerAddressRegionPickerOpen
}
function addressRegionSearchKeywordRef(target: AddressRegionPickerTarget) {
  return target === 'sale' ? addressRegionSearchKeyword : quickCustomerAddressRegionSearchKeyword
}
function addressRegionSearchLoadingRef(target: AddressRegionPickerTarget) {
  return target === 'sale' ? addressRegionSearchLoading : quickCustomerAddressRegionSearchLoading
}
function addressRegionSearchResultsRef(target: AddressRegionPickerTarget) {
  return target === 'sale' ? addressRegionSearchResults : quickCustomerAddressRegionSearchResults
}
async function openAddressRegionPicker(target: AddressRegionPickerTarget) {
  clearAddressRegionSearch(target)
  await loadAddressRootOptions()
  const path = addressRegionPathRef(target).value
  if (path.length) await ensureAddressPathLoaded(path)
  await refreshAddressRegionOptionsForSearch()
}
function clearAddressRegionSearch(target: AddressRegionPickerTarget) {
  if (addressRegionSearchTimers[target]) {
    clearTimeout(addressRegionSearchTimers[target])
    addressRegionSearchTimers[target] = undefined
  }
  addressRegionSearchKeywordRef(target).value = ''
  addressRegionSearchResultsRef(target).value = []
  addressRegionSearchLoadingRef(target).value = false
}
function handleAddressRegionSearch(target: AddressRegionPickerTarget) {
  const keywordRef = addressRegionSearchKeywordRef(target)
  const keyword = normalizeAddressSearchText(keywordRef.value)
  const loadingRef = addressRegionSearchLoadingRef(target)
  const resultsRef = addressRegionSearchResultsRef(target)
  if (addressRegionSearchTimers[target]) clearTimeout(addressRegionSearchTimers[target])
  if (!keyword) {
    loadingRef.value = false
    resultsRef.value = []
    return
  }
  const seq = ++addressRegionSearchSeq[target]
  loadingRef.value = true
  addressRegionSearchTimers[target] = window.setTimeout(async () => {
    try {
      await loadAddressRootOptions()
      const list = await searchAddressRegions(keywordRef.value, 80)
      if (seq !== addressRegionSearchSeq[target]) return
      await Promise.all(list.map(item => ensureAddressPathLoaded(item.path?.length ? item.path : [item.code])))
      resultsRef.value = list
    } catch (error) {
      if (seq === addressRegionSearchSeq[target]) ElMessage.error('行政区搜索失败')
    } finally {
      if (seq === addressRegionSearchSeq[target]) loadingRef.value = false
    }
  }, 260)
}
function clearAddressRegion(target: AddressRegionPickerTarget, form: AddressParseResult) {
  addressRegionPathRef(target).value = []
  form.province = ''
  form.city = ''
  form.district = ''
  form.street = ''
  form.village = ''
  form.regionPath = []
  form.regionPathNames = []
  clearAddressRegionSearch(target)
}
async function selectAddressRegionSearchResult(target: AddressRegionPickerTarget, form: AddressParseResult, option: AddressRegionOption, trimDetail = false) {
  const path = (option.path?.length ? option.path : [option.code]).map(item => String(item || '')).filter(Boolean)
  await ensureAddressPathLoaded(path)
  addressRegionPathRef(target).value = path
  addressRegionChanged(form, path, trimDetail)
  clearAddressRegionSearch(target)
  await keepAddressRegionPickerForNextLevel(target, path)
}
async function selectAddressRegionOption(target: AddressRegionPickerTarget, form: AddressParseResult, columnIndex: number, option: AddressCascaderOption, trimDetail = false) {
  const path = [...addressRegionPathRef(target).value.slice(0, columnIndex), option.code]
  await ensureAddressPathLoaded(path)
  addressRegionPathRef(target).value = path
  addressRegionChanged(form, path, trimDetail)
  clearAddressRegionSearch(target)
  await keepAddressRegionPickerForNextLevel(target, path)
}
async function keepAddressRegionPickerForNextLevel(target: AddressRegionPickerTarget, path: string[]) {
  const current = findAddressOptionByCode(addressAreaOptions.value, path[path.length - 1])
  if (!current || isAddressRegionLeafSelection(path, current)) {
    addressRegionPickerOpenRef(target).value = false
    return
  }
  const children = await attachAddressChildren(current)
  await refreshAddressRegionOptionsForSearch()
  addressRegionPickerOpenRef(target).value = Boolean(children.length)
}
function addressRegionColumns(path: string[]) {
  const columns: AddressCascaderOption[][] = []
  let options = addressAreaOptions.value
  if (options.length) columns.push(options)
  for (const code of path) {
    const current = options.find(item => item.code === code) || findAddressOptionByCode(addressAreaOptions.value, code)
    if (!current?.children?.length || current.leaf) break
    options = current.children
    columns.push(options)
  }
  return columns
}
function addressRegionSelectionText(result?: AddressParseResult) {
  return visibleAddressRegionNames(result).join(' / ')
}
function addressRegionOptionPathText(option: AddressRegionOption) {
  const names = option.pathNames?.length ? option.pathNames : [option.name]
  return names.join(' / ')
}
function findAddressOptionByCode(options: AddressCascaderOption[], code: string): AddressCascaderOption | undefined {
  for (const option of options) {
    if (option.code === code) return option
    const child = option.children?.length ? findAddressOptionByCode(option.children, code) : undefined
    if (child) return child
  }
  return undefined
}
function findAddressOptionByName(options: AddressCascaderOption[], name?: string) {
  const normalizedName = normalizeRegionName(name)
  if (!normalizedName) return undefined
  return options.find(option => {
    const optionName = normalizeRegionName(option.name)
    return option.name === name || optionName === normalizedName || option.name.includes(name || '') || String(name || '').includes(option.name)
  })
}
function findDirectCityOption(options: AddressCascaderOption[], provinceName?: string) {
  if (options.length === 1 && ['市辖区', '县'].includes(options[0].name)) {
    return options[0]
  }
  return findAddressOptionByName(options, provinceName)
}
function findStreetOptionInText(options: AddressCascaderOption[], detailAddress?: string) {
  const text = String(detailAddress || '')
  if (!text) return undefined
  return options.find(option => text.includes(option.name) || text.includes(normalizeStreetName(option.name)))
}
function normalizeRegionName(name?: string) {
  return String(name || '').trim().replace(/\s+/g, '')
}
function normalizeStreetName(name?: string) {
  return normalizeRegionName(name).replace(/办事处$/, '')
}
function contactCandidateOptions(result?: AddressParseResult) {
  return Array.from(new Set((result?.contactCandidates || []).map(item => String(item || '').trim()).filter(Boolean)))
}
function visibleAddressRegionNames(result?: AddressParseResult) {
  return [result?.province, result?.city, result?.district, result?.street, result?.village]
    .filter(Boolean)
    .filter((item, index) => !(index === 1 && ['市辖区', '县'].includes(String(item))))
    .map(item => String(item))
}
function addressRegionText(result?: AddressParseResult) {
  return visibleAddressRegionNames(result).join('')
}
function addressFullText(result?: AddressParseResult) {
  if (!result) return ''
  const regionText = addressRegionText(result)
  const detailText = trimAddressDetailByRegionNames(result.detailAddress, visibleAddressRegionNames(result))
  if (!detailText) return regionText
  if (!regionText) {
    return detailText
  }
  return `${regionText}${detailText}`
}
function trimAddressDetailByRegionNames(detail?: string, names: string[] = []) {
  let result = String(detail || '').trim()
  if (!result || !names.length) return result
  names.forEach(name => {
    if (['市辖区', '县'].includes(name)) return
    result = removeLeadingRegionName(result, name)
  })
  return result.trim()
}
function removeLeadingRegionName(text: string, name: string) {
  let result = text
  for (const alias of regionNameAliases(name)) {
    const next = removeLeadingTextIgnoreSpaces(result, alias)
    if (next !== result) {
      result = next
      break
    }
  }
  return result.trim()
}
function removeLeadingTextIgnoreSpaces(text: string, prefix: string) {
  const normalizedPrefix = normalizeRegionName(prefix)
  if (!text || !normalizedPrefix) return text
  let compact = ''
  const indexes: number[] = []
  Array.from(text).forEach((char, index) => {
    if (/\s/.test(char)) return
    compact += char
    indexes.push(index)
  })
  if (!compact.startsWith(normalizedPrefix)) return text
  const endIndex = indexes[Array.from(normalizedPrefix).length - 1]
  if (endIndex === undefined) return text
  return text.slice(endIndex + 1).trim()
}
function regionNameAliases(name: string) {
  const normalized = normalizeRegionName(name)
  const aliases = new Set<string>([normalized])
  ;['街道办事处', '街道', '镇', '乡', '苏木', '社区居民委员会', '社区居委会', '居民委员会', '村民委员会', '村委会', '委员会'].forEach(suffix => {
    if (normalized.endsWith(suffix) && normalized.length > suffix.length) {
      aliases.add(normalized.slice(0, -suffix.length))
    }
  })
  return Array.from(aliases).filter(Boolean)
}
function isVirtualPhone(phone?: string) {
  return /[-－—转#]/.test(phone || '')
}
async function submitQuickCustomer() {
  const valid = await quickCustomerFormRef.value?.validate().catch(() => false)
  if (!valid) return
  quickCustomerForm.name = quickCustomerForm.contact || quickCustomerForm.phone || quickCustomerForm.code
  quickCustomerSaving.value = true
  try {
    await addMaster('customer', quickCustomerForm)
    const [latestPartners, createdPage] = await Promise.all([
      loadPartners(),
      listMaster('customer', { current: 1, size: 1, code: quickCustomerForm.code })
    ])
    const created = createdPage.records[0] || latestPartners.find(item => item.code === quickCustomerForm.code)
    if (created) {
      form.value.partnerId = created.id
    }
    if (!form.value.receiverName && quickCustomerForm.contact) form.value.receiverName = quickCustomerForm.contact
    if (!form.value.receiverPhone && quickCustomerForm.phone) form.value.receiverPhone = quickCustomerForm.phone
    if (!form.value.receiverAddress && quickCustomerForm.address) form.value.receiverAddress = quickCustomerForm.address
    ElMessage.success('客户新增成功')
    quickCustomerOpen.value = false
  } finally {
    quickCustomerSaving.value = false
  }
}
function resetQuickWarehouse() {
  quickWarehouseForm.code = `WH_${Date.now().toString().slice(-8)}`
  quickWarehouseForm.name = ''
  quickWarehouseForm.parentId = '0'
  quickWarehouseForm.sortOrder = 0
  quickWarehouseForm.status = 1
  quickWarehouseForm.remark = ''
}
function openQuickWarehouse() {
  resetQuickWarehouse()
  quickWarehouseOpen.value = true
}
async function submitQuickWarehouse() {
  const valid = await quickWarehouseFormRef.value?.validate().catch(() => false)
  if (!valid) return
  quickWarehouseSaving.value = true
  try {
    await addMaster('warehouse', quickWarehouseForm)
    const [latestWarehouses, createdPage] = await Promise.all([
      loadWarehouses(),
      listMaster('warehouse', { current: 1, size: 1, code: quickWarehouseForm.code })
    ])
    const created = createdPage.records[0] || latestWarehouses.find(item => item.code === quickWarehouseForm.code)
    if (created) {
      form.value.warehouseId = created.id
    }
    ElMessage.success('仓库新增成功')
    quickWarehouseOpen.value = false
  } finally {
    quickWarehouseSaving.value = false
  }
}
function resetQuickEmployee() {
  quickEmployeeForm.username = ''
  quickEmployeeForm.nickname = ''
  quickEmployeeForm.password = ''
  quickEmployeeForm.status = 1
  quickEmployeeForm.roleIds = []
}
async function openQuickEmployee() {
  if (!canManageEmployee.value) return
  resetQuickEmployee()
  try {
    const role = await ensureSalespersonRole()
    quickEmployeeForm.roleIds = [String(role.roleId)]
    quickEmployeeOpen.value = true
  } catch (error: any) {
    ElMessage.error(error?.message || '业务员角色加载失败')
  }
}
async function submitQuickEmployee() {
  const valid = await quickEmployeeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  quickEmployeeSaving.value = true
  try {
    const role = await ensureSalespersonRole()
    const username = quickEmployeeForm.username
    await addStaff({ ...quickEmployeeForm, roleIds: [String(role.roleId)] })
    const latestEmployees = await loadEmployeeUsers()
    const created = latestEmployees.find(item => item.username === username)
    if (created) {
      employeeChanged(created.userId)
    }
    ElMessage.success('业务员新增成功')
    quickEmployeeOpen.value = false
  } finally {
    quickEmployeeSaving.value = false
  }
}
function submit() {
  if (isProduction.value) {
    submitProduction()
    return
  }
  if (readonly.value) return
  const missing = missingRequiredFields()
  if (missing.length) {
    ElMessage.warning(`请完善必填项：${missing.join('、')}`)
    formRef.value?.validate(() => undefined)
    return
  }
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    calc()
    const payload = sanitizeBill()
    const action = payload.id ? updateBill(module.value, payload) : addBill(module.value, payload)
    action.then(() => {
      if (module.value === 'sale') {
        clearSaleDraft()
      }
      skipLeavePrompt.value = true
      ElMessage.success('保存成功')
      back()
    })
  })
}
function submitProduction() {
  if (!form.value.id) {
    ElMessage.warning('生产单只能维护已有销售单')
    return
  }
  if (!productionEditable.value) {
    ElMessage.warning('没有生产单维护权限')
    return
  }
  updateProductionBill(form.value.id, {
    productionProgress: form.value.productionProgress || '',
    trackingNo: form.value.trackingNo || '',
    productionUserName: form.value.productionUserName || ''
  }).then(() => {
    ElMessage.success('保存成功')
    back()
  })
}
function missingRequiredFields() {
  const missing: string[] = []
  if (!form.value.billDate) missing.push('日期')
  if (!form.value.partnerId) missing.push(partnerLabel.value)
  if (!form.value.warehouseId) missing.push('仓库')
  if (!form.value.items.length) {
    missing.push('商品明细')
    return missing
  }
  form.value.items.forEach((row, index) => {
    const prefix = `商品${index + 1}`
    if (!row.productId) missing.push(`${prefix}商品`)
    if (Number(row.qty || 0) <= 0) missing.push(`${prefix}数量`)
    if (row.price === undefined || row.price === null || Number(row.price) < 0) missing.push(`${prefix}单价`)
  })
  if (Number(form.value.paidAmount || 0) > 0) {
    if (!form.value.accountId) missing.push('账户')
    if (!form.value.paymentMethod) missing.push('付款方式')
  }
  return missing
}
function sanitizeBill() {
  form.value.items.forEach(syncRowSnapshot)
  return {
    ...form.value,
    items: form.value.items.map(({ optionProducts, attributeSelections, purchasePrice, basePrice, availableAttributeIds, availableAttributeText, productOptionsRequestId, ...item }) => item)
  } as ErpBill
}
function back() { router.push(`/erp/${module.value}/list`) }
watch(form, queueSaleDraftSave, { deep: true })
onBeforeRouteLeave((_to, _from, next) => {
  if (skipLeavePrompt.value || !hasUnsavedSaleDraft()) {
    next()
    return
  }
  saveSaleDraftNow()
  ElMessageBox.confirm('当前销售单还没有保存，已自动保留草稿。确定离开吗？', '未保存草稿', {
    confirmButtonText: '离开',
    cancelButtonText: '继续填写',
    type: 'warning'
  }).then(() => next()).catch(() => next(false))
})
onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  await loadOptions()
  loadData()
})
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  if (draftSaveTimer) {
    clearTimeout(draftSaveTimer)
    draftSaveTimer = undefined
  }
  if (draftStatusTimer) {
    clearTimeout(draftStatusTimer)
    draftStatusTimer = undefined
  }
})
</script>

<style scoped>
.card-header, .toolbar, .totals, .section-title { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.bill-form-title,
.card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bill-form-title {
  min-width: 0;
}
.draft-save-status {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 400;
}
.draft-save-status.is-saving {
  color: var(--el-color-primary);
}
.draft-save-status.is-saved {
  color: var(--el-color-success);
}
.draft-save-status.is-error {
  color: var(--el-color-danger);
}
.draft-count {
  min-width: 16px;
  height: 16px;
  margin-left: 4px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--el-color-danger);
  color: #fff;
  font-size: 11px;
  line-height: 16px;
}
.sale-draft-popover {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sale-draft-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.sale-draft-head span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.sale-draft-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 12px;
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
}
.sale-draft-summary div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.sale-draft-summary span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.sale-draft-summary strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.sale-draft-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.toolbar { justify-content: flex-start; margin-bottom: 12px; }
.section-title { margin-bottom: 16px; }
.totals { justify-content: flex-end; padding: 16px 0; font-weight: 600; }
.bill-item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.bill-item-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 14px;
  background: var(--el-bg-color);
}
.bill-item-head,
.bill-item-product {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bill-item-head {
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
}
.bill-item-product {
  margin-bottom: 12px;
}
.bill-item-product-select {
  flex: 1;
  margin-bottom: 0;
}
.bill-item-path {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.attribute-group-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px 12px;
}
.attribute-group-field {
  min-width: 0;
}
.attribute-group-label {
  display: block;
  margin-bottom: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}
.product-option {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
}
.product-option-image,
.product-option-empty,
.bill-product-image {
  width: 44px;
  height: 44px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
  flex: 0 0 auto;
}
.bill-product-placeholder {
  width: 44px;
  height: 44px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  flex: 0 0 auto;
}
.product-option-image,
.bill-product-image {
  object-fit: cover;
}
.product-option-empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}
.bill-logo-field {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
.bill-logo-uploader :deep(.el-upload) {
  display: block;
}
.bill-logo-box {
  width: 88px;
  height: 88px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
}
.bill-logo-box.has-image {
  border-style: solid;
}
.bill-logo-preview {
  display: block;
  width: 88px;
  height: 88px;
  object-fit: cover;
}
.bill-logo-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.bill-logo-placeholder .el-icon {
  font-size: 22px;
}
.product-option-text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}
.product-option-text span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.image-empty {
  color: var(--el-text-color-placeholder);
}
.employee-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-width: 0;
}
.employee-option-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.select-empty-action {
  min-height: 42px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--el-text-color-secondary);
}
.address-parse-result {
  margin-bottom: 12px;
  padding: 14px;
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.address-parse-tip {
  padding: 8px 10px;
  border-radius: 4px;
  background: #fff7e6;
  color: #d46b08;
  line-height: 20px;
}
.address-contact-line {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(220px, 2fr);
  gap: 10px;
}
.address-phone-field {
  min-width: 0;
}
.address-phone-hint {
  display: block;
  margin-top: 4px;
  color: #ff7a45;
  font-size: 12px;
  line-height: 18px;
}
.address-candidate-box {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.address-region-picker-input {
  width: 100%;
}
.address-region-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
:global(.address-region-popper) {
  padding: 12px !important;
}
.address-region-columns {
  display: flex;
  min-height: 220px;
  max-height: 300px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  overflow-x: auto;
  overflow-y: hidden;
  background: var(--el-bg-color);
}
.address-region-column {
  width: 150px;
  min-width: 150px;
  padding: 6px;
  overflow-y: auto;
  border-right: 1px solid var(--el-border-color-lighter);
}
.address-region-column:last-child {
  border-right: none;
}
.address-region-option,
.address-region-result {
  width: 100%;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--el-text-color-primary);
  cursor: pointer;
  text-align: left;
}
.address-region-option {
  display: block;
  padding: 7px 8px;
  line-height: 18px;
}
.address-region-option:hover,
.address-region-option.active,
.address-region-result:hover {
  background: var(--el-fill-color-light);
  color: var(--el-color-primary);
}
.address-region-search-results {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 6px;
}
.address-region-result {
  display: block;
  padding: 8px 10px;
  line-height: 20px;
}
.address-region-empty {
  padding: 18px 0;
  color: var(--el-text-color-secondary);
  text-align: center;
}
.address-normalized-preview {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  word-break: break-all;
}
</style>
