<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <s-table
        ref="table"
        size="default"
        rowKey="skuId"
        :columns="columns"
        :data="loadData"
        :alert="true"
        :rowSelection="rowSelection"
        showPagination="auto"
        :pageSize="300"
      >
        <span slot="serial" slot-scope="text, record, index">
          {{ index + 1 }}
        </span>
        <span slot="status" slot-scope="text">
          <a-badge :status="text | statusTypeFilter" :text="text | statusFilter" />
        </span>
        <span slot="description" slot-scope="text">
          <ellipsis :length="60" tooltip>{{ text }}</ellipsis>
        </span>
        <span slot="soldPrice" slot-scope="text, record">
          <a-input-number style="margin: -5px 0" :value="text" @blur="(e) => handleChange(e.target.value, record)" />
        </span>
      </s-table>

      <create-form
        ref="createModal"
        :visible="visible"
        :loading="confirmLoading"
        :model="mdl"
        @cancel="handleCancel"
        @ok="handleOk"
      />
      <step-by-step-modal ref="modal" @ok="handleOk" />
    </a-card>
  </page-header-wrapper>
</template>

<script>
import moment from 'moment'
import { STable, Ellipsis } from '@/components'
import { getRoleList } from '@/api/manage'

import StepByStepModal from './modules/StepByStepModal'
import CreateForm from './modules/CreateForm'
import request from '@/utils/request'

const columns = [
  {
    title: '#',
    scopedSlots: { customRender: 'serial' },
  },
  {
    title: 'ID',
    dataIndex: 'skuId',
  },
  {
    title: '名称',
    dataIndex: 'name',
    scopedSlots: { customRender: 'description' },
  },
  {
    title: '状态',
    dataIndex: 'desc',
  },
  {
    title: '批发价格',
    dataIndex: 'wprice',
  },
  {
    title: '提醒价格',
    dataIndex: 'notifyPrice',
  },
  {
    title: '出货价格',
    dataIndex: 'soldPrice',
    scopedSlots: { customRender: 'soldPrice' },
  },
  {
    title: '更新时间',
    dataIndex: 'lastUpdateTs',
    customRender: (ts) => moment(ts).format('YYYY-MM-DD HH:mm:ss'),
  },
]

const statusMap = {
  0: {
    status: 'default',
    text: '关闭',
  },
  1: {
    status: 'processing',
    text: '运行中',
  },
  2: {
    status: 'success',
    text: '已上线',
  },
  3: {
    status: 'error',
    text: '异常',
  },
}

export default {
  name: 'TableList',
  components: {
    STable,
    Ellipsis,
    CreateForm,
    StepByStepModal,
  },
  data() {
    this.columns = columns
    return {
      // create model
      visible: false,
      confirmLoading: false,
      mdl: null,
      // 高级搜索 展开/关闭
      advanced: false,
      // 查询参数
      queryParam: {},
      moutai: null,
      profit: null,
      // 加载数据方法 必须为 Promise 对象
      loadData: (parameter) => {
        const requestParameters = Object.assign({}, parameter, this.queryParam)
        return request({
          url: '/sku/list?sort=true',
          method: 'get',
          params: requestParameters,
        })
          .then((res) => {
            return res.result
          })
          .catch((err) => {
            console.log(err)
          })
      },
      selectedRowKeys: [],
      selectedRows: [],
    }
  },
  filters: {
    statusFilter(type) {
      return statusMap[type].text
    },
    statusTypeFilter(type) {
      return statusMap[type].status
    },
  },
  created() {
    getRoleList({ t: new Date() })
  },
  computed: {
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        onChange: this.onSelectChange,
      }
    },
  },
  mounted() {
    this.getConfig()
  },
  methods: {
    getConfig() {
      const that = this
      return request({
        url: '/config/map',
        method: 'get',
      }).then((res) => {
          that.moutai = res.result.moutai
          that.profit = res.result.profit
          console.log("初始化系统配置", that.moutai, that.profit)
        })
        .catch((err) => {
          console.log(err)
        })
    },
    query(queryParam) {
      return request({
        url: '/sku/list?sort=true',
        method: 'get',
        params: queryParam,
      })
        .then((res) => {
          return res.result
        })
        .catch((err) => {
          console.log(err)
        })
    },
    handleChange(value, record) {
      if (!value){
        return
      }
      const that = this
      console.log(value, that.profit, that.moutai)
      record.notifyPrice = parseInt((value - that.profit)*6000/(7499-that.moutai))
      record.soldPrice = value
      const param = { skuId: record.skuId, soldPrice: value }
      request({
        url: '/sku/price-sold',
        method: 'put',
        params: param,
      })
        .then((res) => {
          console.log(res)
          that.query()
        })
        .catch((err) => {
          console.log(err)
        })
    },
    handleAdd() {
      this.mdl = null
      this.visible = true
    },
    handleEdit(record) {
      this.visible = true
      this.mdl = { ...record }
    },
    handleOk() {
      const form = this.$refs.createModal.form
      this.confirmLoading = true
      form.validateFields((errors, values) => {
        if (!errors) {
          console.log('values', values)
          if (values.id > 0) {
            // 修改 e.g.
            new Promise((resolve, reject) => {
              setTimeout(() => {
                resolve()
              }, 1000)
            }).then((res) => {
              this.visible = false
              this.confirmLoading = false
              // 重置表单数据
              form.resetFields()
              // 刷新表格
              this.$refs.table.refresh()

              this.$message.info('修改成功')
            })
          } else {
            // 新增
            new Promise((resolve, reject) => {
              setTimeout(() => {
                resolve()
              }, 1000)
            }).then((res) => {
              this.visible = false
              this.confirmLoading = false
              // 重置表单数据
              form.resetFields()
              // 刷新表格
              this.$refs.table.refresh()

              this.$message.info('新增成功')
            })
          }
        } else {
          this.confirmLoading = false
        }
      })
    },
    handleCancel() {
      this.visible = false

      const form = this.$refs.createModal.form
      form.resetFields() // 清理表单数据（可不做）
    },
    handleSub(record) {
      if (record.status !== 0) {
        this.$message.info(`${record.no} 订阅成功`)
      } else {
        this.$message.error(`${record.no} 订阅失败，规则已关闭`)
      }
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys
      this.selectedRows = selectedRows
    },
    toggleAdvanced() {
      this.advanced = !this.advanced
    },
    resetSearchForm() {
      this.queryParam = {
        date: moment(new Date()),
      }
    },
  },
}
</script>
